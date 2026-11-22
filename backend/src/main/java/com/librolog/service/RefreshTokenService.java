package com.librolog.service;

import com.librolog.config.JwtProperties;
import com.librolog.entity.RefreshToken;
import com.librolog.entity.User;
import com.librolog.exception.InvalidTokenException;
import com.librolog.repository.RefreshTokenRepository;
import com.librolog.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Gestión de refresh tokens con rotación y detección de reutilización.
 *
 * <p>Cada refresco invalida el token usado y emite uno nuevo. Si llega un token
 * ya revocado se interpreta como robo de sesión y se cierran todas las sesiones
 * del usuario.</p>
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param repository repositorio de refresh tokens
     * @param properties propiedades JWT
     */
    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Resultado de una rotación: usuario y nuevo token en claro.
     *
     * @param user     usuario propietario
     * @param rawToken nuevo refresh token en claro
     */
    public record Rotation(User user, String rawToken) {
    }

    /**
     * Emite un refresh token nuevo para un usuario.
     *
     * @param user      usuario
     * @param userAgent agente de usuario de la petición
     * @param ipAddress dirección IP de la petición
     * @return token en claro, que no se vuelve a poder recuperar
     */
    @Transactional
    public String issue(User user, String userAgent, String ipAddress) {
        return persist(user, userAgent, ipAddress);
    }

    /**
     * Rota un refresh token: lo revoca y emite otro.
     *
     * @param rawToken  token recibido en la cookie
     * @param userAgent agente de usuario de la petición
     * @param ipAddress dirección IP de la petición
     * @return usuario y nuevo token
     * @throws InvalidTokenException si el token no existe, caducó o fue revocado
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public Rotation rotate(String rawToken, String userAgent, String ipAddress) {
        RefreshToken current = repository.findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .orElseThrow(InvalidTokenException::new);

        if (current.getRevokedAt() != null) {
            // Reutilización de un token ya usado: posible robo. Se cierran todas
            // las sesiones del usuario.
            repository.revokeAllByUserId(current.getUser().getId(), Instant.now());
            throw new InvalidTokenException();
        }

        if (current.getExpiresAt().isBefore(Instant.now())) {
            current.setRevokedAt(Instant.now());
            throw new InvalidTokenException();
        }

        current.setRevokedAt(Instant.now());
        User user = current.getUser();
        String newRawToken = persist(user, userAgent, ipAddress);
        return new Rotation(user, newRawToken);
    }

    /**
     * Revoca un refresh token si existe. La operación es idempotente.
     *
     * @param rawToken token recibido en la cookie; puede ser {@code null}
     */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHash(TokenHasher.sha256Hex(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
            }
        });
    }

    private String persist(User user, String userAgent, String ipAddress) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(TokenHasher.sha256Hex(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(properties.refreshTokenDays() * 86_400L));
        token.setUserAgent(truncate(userAgent, 255));
        token.setIpAddress(truncate(ipAddress, 45));
        repository.save(token);

        return rawToken;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
