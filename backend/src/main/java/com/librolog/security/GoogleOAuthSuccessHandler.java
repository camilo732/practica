package com.librolog.security;

import com.librolog.config.JwtProperties;
import com.librolog.config.SecurityProperties;
import com.librolog.service.AuthService;
import com.librolog.service.GoogleAccount;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Completa el acceso con Google y entrega la sesión a la aplicación.
 *
 * <p>Google autentica a la persona en el servidor. Este manejador crea el
 * refresh token, lo guarda en la cookie HttpOnly y redirige a una página
 * estática que canjea la cookie por un token de acceso en memoria. El token de
 * acceso nunca pasa por la URL.</p>
 */
@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthSuccessHandler.class);
    private static final String CALLBACK_PAGE = "/oauth2-callback.html";

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;

    /**
     * Crea el manejador con sus dependencias.
     *
     * @param authService        servicio de autenticación
     * @param jwtProperties      configuración de tokens
     * @param securityProperties configuración de seguridad
     */
    public GoogleOAuthSuccessHandler(AuthService authService,
                                     JwtProperties jwtProperties,
                                     SecurityProperties securityProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
        this.securityProperties = securityProperties;
    }

    /**
     * Crea la sesión y redirige a la página de canje.
     *
     * @param request        petición HTTP
     * @param response       respuesta HTTP
     * @param authentication autenticación de Google
     * @throws IOException      si falla la redirección
     * @throws ServletException si falla el manejo de la sesión
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        try {
            if (!(authentication.getPrincipal() instanceof OAuth2User principal)) {
                throw new IllegalStateException("La autenticación de Google no contiene un usuario.");
            }

            GoogleAccount account = toAccount(principal.getAttributes());
            AuthService.AuthResult result = authService.loginWithGoogle(
                    account,
                    request.getHeader(HttpHeaders.USER_AGENT),
                    request.getRemoteAddr());

            boolean secure = isSecure(request);
            response.addHeader(HttpHeaders.SET_COOKIE, RefreshTokenCookies.create(
                    result.refreshToken(), jwtProperties.refreshTokenDays(), secure).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, OAuthNextCookie.clear(secure).toString());

            String next = OAuthNextCookie.read(request);
            String target = CALLBACK_PAGE
                    + (next == null ? "" : "?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8));

            invalidateSession(request);
            response.sendRedirect(target);
        } catch (RuntimeException ex) {
            log.warn("No se pudo completar el acceso con Google: {}", ex.getMessage());
            invalidateSession(request);
            response.sendRedirect("/login.html?oauth=error");
        }
    }

    private static GoogleAccount toAccount(Map<String, Object> attributes) {
        String subject = text(attributes.get("sub"));
        String email = text(attributes.get("email"));
        String name = text(attributes.get("name"));
        String locale = text(attributes.get("locale"));

        return new GoogleAccount(
                subject,
                email,
                isTrue(attributes.get("email_verified")),
                name,
                locale);
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean isSecure(HttpServletRequest request) {
        return request.isSecure() || securityProperties.requireHttps();
    }

    private static void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
