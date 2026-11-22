package com.librolog.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utilidades de hash para tokens.
 *
 * <p>Los refresh tokens se guardan como SHA-256 en hexadecimal. No se usa
 * BCrypt aquí a propósito: el token ya es aleatorio de 256 bits, así que un
 * hash rápido evita penalizar cada refresco de sesión.</p>
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    /**
     * Calcula el hash SHA-256 de un token en hexadecimal minúscula.
     *
     * @param token token en claro
     * @return hash hexadecimal de 64 caracteres
     */
    public static String sha256Hex(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM.", ex);
        }
    }
}
