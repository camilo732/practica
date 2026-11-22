package com.librolog.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del cifrado de contraseñas.
 */
class PasswordEncoderTest {

    /**
     * Comprueba que BCrypt usa coste 12 y valida correctamente.
     */
    @Test
    void usaCosteDoceYValidaLaContrasena() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        String hash = encoder.encode("Lectura2026");

        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$")
                || hash.startsWith("$2y$12$"), "El hash debe declarar coste 12: " + hash);
        assertTrue(encoder.matches("Lectura2026", hash));
        assertFalse(encoder.matches("Lectura2027", hash));
    }
}
