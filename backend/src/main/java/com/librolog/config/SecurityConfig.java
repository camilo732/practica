package com.librolog.config;

import com.librolog.repository.UserRepository;
import com.librolog.security.ContentSecurityPolicyWriter;
import com.librolog.security.GoogleOAuthFailureHandler;
import com.librolog.security.GoogleOAuthSuccessHandler;
import com.librolog.security.HttpsRequiredFilter;
import com.librolog.security.JwtAuthenticationFilter;
import com.librolog.security.JwtService;
import com.librolog.security.RateLimitFilter;
import com.librolog.security.RestAccessDeniedHandler;
import com.librolog.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad de LibroLog.
 *
 * <p>Aplica autenticación JWT sin estado, BCrypt con coste 12, limitación de
 * peticiones en {@code /api/auth/**}, cabeceras de seguridad y CORS con
 * orígenes explícitos.</p>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class, GoogleOAuthProperties.class})
public class SecurityConfig {

    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "base-uri 'self'",
            "form-action 'self'",
            "object-src 'none'",
            "frame-ancestors 'none'",
            "img-src 'self' data: https:",
            "style-src 'self' 'unsafe-inline'",
            "script-src 'self'");

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final SecurityProperties securityProperties;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final GoogleOAuthSuccessHandler googleOAuthSuccessHandler;
    private final GoogleOAuthFailureHandler googleOAuthFailureHandler;

    /**
     * Crea la configuración con sus dependencias.
     *
     * @param jwtService                servicio de tokens
     * @param userRepository            repositorio de usuarios
     * @param authenticationEntryPoint  respuesta 401
     * @param accessDeniedHandler       respuesta 403
     * @param securityProperties        configuración de seguridad
     * @param googleOAuthProperties     credenciales de Google
     * @param clientRegistrationRepository registro de clientes OAuth2
     * @param googleOAuthSuccessHandler manejador de éxito de Google
     * @param googleOAuthFailureHandler manejador de fallo de Google
     */
    public SecurityConfig(JwtService jwtService,
                          UserRepository userRepository,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          SecurityProperties securityProperties,
                          GoogleOAuthProperties googleOAuthProperties,
                          ClientRegistrationRepository clientRegistrationRepository,
                          GoogleOAuthSuccessHandler googleOAuthSuccessHandler,
                          GoogleOAuthFailureHandler googleOAuthFailureHandler) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.securityProperties = securityProperties;
        this.googleOAuthProperties = googleOAuthProperties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.googleOAuthSuccessHandler = googleOAuthSuccessHandler;
        this.googleOAuthFailureHandler = googleOAuthFailureHandler;
    }

    /**
     * Define la cadena de filtros de seguridad.
     *
     * @param http configuración HTTP de Spring Security
     * @return cadena de filtros configurada
     * @throws Exception si la configuración no es válida
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userRepository);

        http
                // La API es stateless: no hay sesión de servidor ni formulario.
                // El refresh token va en cookie SameSite=Strict y las peticiones
                // que la usan exigen la cabecera X-LibroLog-Client.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny())
                            .contentTypeOptions(Customizer.withDefaults())
                            .referrerPolicy(referrer ->
                                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                    // HSTS se desactiva en el perfil local: con un certificado de
                    // confianza el navegador fijaría localhost a HTTPS durante un
                    // año y rompería otros servicios locales en HTTP.
                    if (securityProperties.hstsEnabled()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000));
                    } else {
                        headers.httpStrictTransportSecurity(hsts -> hsts.disable());
                    }
                    headers.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                    "geolocation=(), microphone=(), camera=(), payment=()"))
                            .addHeaderWriter(new ContentSecurityPolicyWriter(CONTENT_SECURITY_POLICY));
                })
                .authorizeHttpRequests(auth -> auth
                        // Las páginas son estáticas y no exponen datos: el acceso a los
                        // datos sigue protegido por la API. El guardián de sesión del
                        // navegador redirige al login, pero la autorización real es JWT.
                        .requestMatchers("/", "/*.html", "/favicon.ico", "/favicon.svg", "/error")
                        .permitAll()
                        .requestMatchers("/css/**", "/js/**", "/assets/**", "/vendor/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/providers",
                                "/api/auth/oauth2/start").permitAll()
                        .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        if (googleOAuthProperties.isConfigured()) {
            http.oauth2Login(oauth -> oauth
                    .clientRegistrationRepository(clientRegistrationRepository)
                    .loginPage("/login.html")
                    .authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestRepository(
                                    new HttpSessionOAuth2AuthorizationRequestRepository()))
                    .securityContextRepository(new RequestAttributeSecurityContextRepository())
                    .successHandler(googleOAuthSuccessHandler)
                    .failureHandler(googleOAuthFailureHandler));
        }

        return http.build();
    }

    /**
     * Configura CORS con orígenes explícitos (nunca {@code *}).
     *
     * @param origins orígenes permitidos separados por coma
     * @return fuente de configuración CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String[] origins) {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(origins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Accept-Language",
                "X-Requested-With", "X-LibroLog-Client"));
        configuration.setExposedHeaders(List.of("Content-Disposition", "Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Registra el filtro que exige HTTPS antes que la cadena de seguridad.
     *
     * @return registro del filtro
     */
    @Bean
    public FilterRegistrationBean<HttpsRequiredFilter> httpsRequiredFilterRegistration() {
        FilterRegistrationBean<HttpsRequiredFilter> registration =
                new FilterRegistrationBean<>(new HttpsRequiredFilter(securityProperties.requireHttps()));
        registration.setOrder(-200);
        return registration;
    }

    /**
     * Registra el filtro de límite de peticiones de autenticación.
     *
     * @return registro del filtro
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(securityProperties.authRateLimitPerMinute()));
        registration.setOrder(-190);
        return registration;
    }
}
