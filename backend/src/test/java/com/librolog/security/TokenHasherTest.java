package com.librolog.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del hash de refresh tokens.
 */
class TokenHasherTest {

    /**
     * Comprueba que el hash es SHA-256 en hexadecimal minúscula.
     */
    @Test
    void produceSha256EnHexadecimal() {
        String hash = TokenHasher.sha256Hex("token-de-prueba");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("^[0-9a-f]{64}$"));
    }

    /**
     * Comprueba que el mismo token produce siempre el mismo hash.
     */
    @Test
    void esDeterminista() {
        assertEquals(TokenHasher.sha256Hex("abc"), TokenHasher.sha256Hex("abc"));
    }

    /**
     * Comprueba que tokens distintos producen hashes distintos.
     */
    @Test
    void distingueTokensDistintos() {
        assertNotEquals(TokenHasher.sha256Hex("abc"), TokenHasher.sha256Hex("abd"));
    }
}
