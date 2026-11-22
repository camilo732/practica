package com.librolog.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas de validación de la ruta de retorno de OAuth2.
 */
class OAuthNextCookieTest {

    /**
     * Acepta una página interna con parámetros.
     */
    @Test
    void acceptsInternalPage() {
        assertEquals("/library.html?status=READ",
                OAuthNextCookie.safeNext("/library.html?status=READ"));
        assertEquals("/admin.html", OAuthNextCookie.safeNext("/admin.html"));
    }

    /**
     * Rechaza destinos externos y rutas peligrosas.
     */
    @Test
    void rejectsExternalOrUnsafePaths() {
        assertNull(OAuthNextCookie.safeNext("https://evil.example/"));
        assertNull(OAuthNextCookie.safeNext("//evil.example/"));
        assertNull(OAuthNextCookie.safeNext("/\\evil.example"));
        assertNull(OAuthNextCookie.safeNext("/dashboard.html\nSet-Cookie: x=y"));
        assertNull(OAuthNextCookie.safeNext("/api/admin/users"));
        assertNull(OAuthNextCookie.safeNext("/login.html"));
        assertNull(OAuthNextCookie.safeNext(null));
    }
}
