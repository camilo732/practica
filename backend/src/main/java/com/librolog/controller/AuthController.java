package com.librolog.controller;

import com.librolog.config.JwtProperties;
import com.librolog.config.SecurityProperties;
import com.librolog.config.GoogleOAuthProperties;
import com.librolog.dto.request.LoginRequest;
import com.librolog.dto.request.RegisterRequest;
import com.librolog.dto.response.AuthProvidersResponse;
import com.librolog.dto.response.AuthResponse;
import com.librolog.dto.response.UserResponse;
import com.librolog.exception.InvalidTokenException;
import com.librolog.security.AuthenticatedUser;
import com.librolog.security.OAuthNextCookie;
import com.librolog.security.RefreshTokenCookies;
import com.librolog.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Endpoints de autenticación.
 *
 * <p>El token de acceso se devuelve en el cuerpo para que el cliente lo guarde
 * solo en memoria. El refresh token viaja en una cookie HttpOnly con
 * {@code SameSite=Strict}.</p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y gestión de sesiones")
public class AuthController {

    private final AuthService authService;
    private final SecurityProperties securityProperties;
    private final JwtProperties jwtProperties;
    private final GoogleOAuthProperties googleOAuthProperties;

    /**
     * Crea el controlador con sus dependencias.
     *
     * @param authService           servicio de autenticación
     * @param securityProperties    configuración de seguridad
     * @param jwtProperties         configuración de tokens
     * @param googleOAuthProperties credenciales de Google
     */
    public AuthController(AuthService authService,
                          SecurityProperties securityProperties,
                          JwtProperties jwtProperties,
                          GoogleOAuthProperties googleOAuthProperties) {
        this.authService = authService;
        this.securityProperties = securityProperties;
        this.jwtProperties = jwtProperties;
        this.googleOAuthProperties = googleOAuthProperties;
    }

    /**
     * Registra una cuenta local y abre sesión.
     *
     * @param request datos de registro
     * @param http    petición HTTP
     * @return sesión creada con token de acceso y cookie de refresco
     */
    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Registrar usuario",
            description = "Crea una cuenta local con contraseña cifrada y devuelve la sesión iniciada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Usuario o correo ya registrados"),
            @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest http) {
        AuthService.AuthResult result = authService.register(
                request, http.getHeader(HttpHeaders.USER_AGENT), http.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), http).toString())
                .body(result.response());
    }

    /**
     * Devuelve los proveedores de autenticación disponibles.
     *
     * @return acceso local y disponibilidad de Google
     */
    @GetMapping("/providers")
    @SecurityRequirements
    @Operation(summary = "Proveedores de acceso",
            description = "Indica si el acceso local y el acceso con Google están disponibles.")
    @ApiResponse(responseCode = "200", description = "Proveedores devueltos")
    public AuthProvidersResponse providers() {
        return new AuthProvidersResponse(true, googleOAuthProperties.isConfigured());
    }

    /**
     * Inicia el viaje de autorización con Google.
     *
     * <p>Guarda la página de destino en una cookie temporal y redirige al
     * endpoint de autorización de Spring Security. Si Google no está
     * configurado, vuelve al acceso con un aviso.</p>
     *
     * @param next ruta interna a la que volver después del acceso
     * @param http petición HTTP
     * @return redirección al proveedor o al formulario de acceso
     */
    @GetMapping("/oauth2/start")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión con Google",
            description = "Redirige a Google OAuth2. Requiere GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET.")
    @ApiResponses({
            @ApiResponse(responseCode = "302",
                    description = "Redirección al proveedor o al formulario de acceso si Google no está configurado")
    })
    public ResponseEntity<Void> startGoogleOAuth(
            @RequestParam(name = "next", required = false) String next,
            HttpServletRequest http) {

        if (!googleOAuthProperties.isConfigured()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/login.html?oauth=unavailable"))
                    .build();
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/google"));

        org.springframework.http.ResponseCookie nextCookie =
                OAuthNextCookie.create(next, isSecure(http));
        if (nextCookie != null) {
            builder.header(HttpHeaders.SET_COOKIE, nextCookie.toString());
        }
        return builder.build();
    }

    /**
     * Inicia sesión con usuario o correo y contraseña.
     *
     * @param request credenciales
     * @param http    petición HTTP
     * @return sesión iniciada
     */
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión",
            description = "Valida las credenciales y devuelve un token de acceso y una cookie de refresco.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest http) {
        AuthService.AuthResult result = authService.login(
                request, http.getHeader(HttpHeaders.USER_AGENT), http.getRemoteAddr());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), http).toString())
                .body(result.response());
    }

    /**
     * Renueva el token de acceso usando la cookie de refresco.
     *
     * @param refreshToken token de la cookie
     * @param clientHeader cabecera de cliente que impide peticiones CSRF
     * @param http         petición HTTP
     * @return sesión renovada con cookie rotada
     */
    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar sesión",
            description = "Rota el refresh token y devuelve un token de acceso nuevo. "
                    + "Debe enviarse la cabecera X-LibroLog-Client: web.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión renovada"),
            @ApiResponse(responseCode = "400", description = "Falta la cabecera de cliente"),
            @ApiResponse(responseCode = "401", description = "Sesión no válida"),
            @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes")
    })
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenCookies.COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(name = RefreshTokenCookies.CLIENT_HEADER, required = false) String clientHeader,
            HttpServletRequest http) {

        requireClientHeader(clientHeader);

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException();
        }

        AuthService.AuthResult result = authService.refresh(
                refreshToken, http.getHeader(HttpHeaders.USER_AGENT), http.getRemoteAddr());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), http).toString())
                .body(result.response());
    }

    /**
     * Cierra la sesión actual revocando el refresh token.
     *
     * @param refreshToken token de la cookie
     * @param clientHeader cabecera de cliente que impide peticiones CSRF
     * @param http         petición HTTP
     * @return respuesta 204 sin contenido
     */
    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Cerrar sesión",
            description = "Revoca el refresh token y borra la cookie. Es idempotente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesión cerrada"),
            @ApiResponse(responseCode = "400", description = "Falta la cabecera de cliente")
    })
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookies.COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(name = RefreshTokenCookies.CLIENT_HEADER, required = false) String clientHeader,
            HttpServletRequest http) {

        requireClientHeader(clientHeader);
        authService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshTokenCookies.clear(isSecure(http)).toString())
                .build();
    }

    /**
     * Devuelve el usuario autenticado.
     *
     * @param principal usuario publicado por el filtro JWT
     * @return datos públicos del usuario
     */
    @GetMapping("/me")
    @Operation(summary = "Perfil actual",
            description = "Devuelve los datos públicos del usuario dueño del token de acceso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil devuelto"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new InvalidTokenException();
        }
        return authService.currentUser(principal.id());
    }

    private org.springframework.http.ResponseCookie refreshCookie(String rawToken,
                                                                 HttpServletRequest http) {
        return RefreshTokenCookies.create(rawToken, jwtProperties.refreshTokenDays(), isSecure(http));
    }

    private boolean isSecure(HttpServletRequest http) {
        return http.isSecure() || securityProperties.requireHttps();
    }

    private void requireClientHeader(String clientHeader) {
        if (clientHeader == null || !RefreshTokenCookies.CLIENT_HEADER_VALUE.equalsIgnoreCase(clientHeader)) {
            throw new IllegalArgumentException(
                    "Falta la cabecera " + RefreshTokenCookies.CLIENT_HEADER
                            + ": " + RefreshTokenCookies.CLIENT_HEADER_VALUE + ".");
        }
    }
}
