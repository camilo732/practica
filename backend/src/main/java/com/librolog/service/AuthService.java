package com.librolog.service;

import com.librolog.config.SecurityProperties;
import com.librolog.dto.request.LoginRequest;
import com.librolog.dto.request.RegisterRequest;
import com.librolog.dto.response.AuthResponse;
import com.librolog.dto.response.UserResponse;
import com.librolog.entity.Role;
import com.librolog.entity.User;
import com.librolog.entity.enums.AuthProvider;
import com.librolog.entity.enums.RoleName;
import com.librolog.exception.ConflictException;
import com.librolog.exception.InvalidCredentialsException;
import com.librolog.exception.InvalidTokenException;
import com.librolog.repository.RoleRepository;
import com.librolog.repository.UserRepository;
import com.librolog.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

/**
 * Registro de usuarios, inicio de sesión y gestión de sesiones.
 *
 * <p>Aplica las reglas de seguridad de la Fase 2: contraseñas con BCrypt,
 * mensajes de error genéricos, bloqueo temporal tras varios fallos y rotación
 * de refresh tokens.</p>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final GoogleAccountService googleAccountService;
    private final SecurityProperties securityProperties;
    private final String dummyHash;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository       repositorio de usuarios
     * @param roleRepository       repositorio de roles
     * @param passwordEncoder      codificador BCrypt
     * @param jwtService           servicio de tokens
     * @param refreshTokenService  servicio de refresh tokens
     * @param googleAccountService servicio de cuentas de Google
     * @param securityProperties   configuración de seguridad
     */
    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       GoogleAccountService googleAccountService,
                       SecurityProperties securityProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.googleAccountService = googleAccountService;
        this.securityProperties = securityProperties;
        // Se calcula una sola vez: sirve para que un usuario inexistente tarde
        // lo mismo que uno con contraseña incorrecta y no se pueda enumerar.
        this.dummyHash = passwordEncoder.encode("librolog-timing-protection-2026");
    }

    /**
     * Resultado interno de una autenticación.
     *
     * @param response     cuerpo que se devuelve al cliente
     * @param refreshToken refresh token en claro para la cookie
     */
    public record AuthResult(AuthResponse response, String refreshToken) {
    }

    /**
     * Registra una cuenta local y abre sesión.
     *
     * @param request   datos de registro
     * @param userAgent agente de usuario
     * @param ipAddress dirección IP
     * @return sesión iniciada
     * @throws ConflictException si el usuario o el correo ya existen
     */
    @Transactional
    public AuthResult register(RegisterRequest request, String userAgent, String ipAddress) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Ese nombre de usuario ya está en uso.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ese correo ya está registrado.");
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el rol USER en la base de datos. Ejecuta las migraciones de Flyway."));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(normalize(request.fullName()));
        user.setLanguage(request.language() == null ? "es" : request.language());
        user.setEnabled(true);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setFailedAttempts((short) 0);
        user.addRole(userRole);
        userRepository.save(user);

        return openSession(user, userAgent, ipAddress);
    }

    /**
     * Abre una sesión para una cuenta autenticada por Google.
     *
     * @param account   datos verificados de Google
     * @param userAgent agente de usuario
     * @param ipAddress dirección IP
     * @return sesión iniciada
     * @throws com.librolog.exception.OAuthLoginException si la cuenta no puede
     *                                                    iniciar sesión
     */
    @Transactional
    public AuthResult loginWithGoogle(GoogleAccount account, String userAgent, String ipAddress) {
        User user = googleAccountService.resolve(account);
        return openSession(user, userAgent, ipAddress);
    }

    /**
     * Inicia sesión con usuario o correo y contraseña.
     *
     * @param request   credenciales
     * @param userAgent agente de usuario
     * @param ipAddress dirección IP
     * @return sesión iniciada
     * @throws InvalidCredentialsException si las credenciales no son válidas
     */
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResult login(LoginRequest request, String userAgent, String ipAddress) {
        String identifier = request.usernameOrEmail().trim();

        User user = userRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }

        if (user.getPassword() == null || isLocked(user) || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            registerFailedAttempt(user);
            throw new InvalidCredentialsException();
        }

        user.setFailedAttempts((short) 0);
        user.setLockedUntil(null);
        return openSession(user, userAgent, ipAddress);
    }

    /**
     * Rota la sesión a partir del refresh token de la cookie.
     *
     * @param rawToken  refresh token recibido
     * @param userAgent agente de usuario
     * @param ipAddress dirección IP
     * @return sesión renovada
     * @throws InvalidTokenException si el token no es válido
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResult refresh(String rawToken, String userAgent, String ipAddress) {
        RefreshTokenService.Rotation rotation =
                refreshTokenService.rotate(rawToken, userAgent, ipAddress);
        return new AuthResult(createResponse(rotation.user()), rotation.rawToken());
    }

    /**
     * Cierra la sesión actual revocando su refresh token.
     *
     * @param rawToken refresh token de la cookie
     */
    @Transactional
    public void logout(String rawToken) {
        refreshTokenService.revoke(rawToken);
    }

    /**
     * Devuelve los datos públicos de un usuario autenticado.
     *
     * @param userId identificador del usuario
     * @return datos públicos
     * @throws InvalidTokenException si el usuario ya no existe
     */
    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidTokenException::new);
        return UserResponse.from(user);
    }

    private AuthResult openSession(User user, String userAgent, String ipAddress) {
        String rawRefreshToken = refreshTokenService.issue(user, userAgent, ipAddress);
        return new AuthResult(createResponse(user), rawRefreshToken);
    }

    private AuthResponse createResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                "Bearer",
                jwtService.accessTokenSeconds(),
                UserResponse.from(user));
    }

    private boolean isLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        if (attempts >= securityProperties.maxFailedAttempts()) {
            user.setFailedAttempts((short) 0);
            user.setLockedUntil(Instant.now().plusSeconds(securityProperties.lockMinutes() * 60L));
        } else {
            user.setFailedAttempts((short) attempts);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
