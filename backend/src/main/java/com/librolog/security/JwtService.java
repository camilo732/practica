package com.librolog.security;

import com.librolog.config.JwtProperties;
import com.librolog.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio de emisión y validación de tokens JWT.
 *
 * <p>Firma con HMAC-SHA y comprueba el emisor en cada lectura. El token de
 * acceso vive solo en memoria del navegador: nunca se guarda en
 * {@code localStorage} ni en una cookie.</p>
 */
@Service
public class JwtService {

    private static final String ISSUER = "librolog";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long accessTokenSeconds;

    /**
     * Crea el servicio a partir de las propiedades configuradas.
     *
     * @param properties propiedades JWT
     * @throws IllegalStateException si el secreto es demasiado corto
     */
    public JwtService(JwtProperties properties) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos " + MIN_SECRET_BYTES
                            + " caracteres. Revisa tu archivo .env.");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTokenSeconds = properties.accessTokenMinutes() * 60L;
    }

    /**
     * Genera un token de acceso para un usuario.
     *
     * @param user usuario autenticado
     * @return token JWT firmado
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Set<String> roles = new LinkedHashSet<>();
        user.getRoles().forEach(role -> roles.add(role.getName().name()));

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("roles", List.copyOf(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * Valida la firma, el emisor y la caducidad de un token.
     *
     * @param token token JWT recibido
     * @return contenido del token
     * @throws io.jsonwebtoken.JwtException si el token no es válido
     */
    public JwtPayload parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        Set<String> roles = new LinkedHashSet<>();
        Object rawRoles = claims.get("roles");
        if (rawRoles instanceof List<?> list) {
            list.forEach(role -> roles.add(String.valueOf(role)));
        }
        return new JwtPayload(userId, username, Set.copyOf(roles));
    }

    /**
     * Devuelve la vida del token de acceso en segundos.
     *
     * @return segundos de validez
     */
    public long accessTokenSeconds() {
        return accessTokenSeconds;
    }
}
