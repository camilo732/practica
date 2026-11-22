package com.librolog.security;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Construcción de la cookie del refresh token.
 *
 * <p>La cookie es {@code HttpOnly} (JavaScript no puede leerla), {@code Secure}
 * cuando la petición es cifrada y {@code SameSite=Strict} para evitar envíos
 * desde otros sitios. Su ruta se limita a {@code /api/auth}.</p>
 */
public final class RefreshTokenCookies {

    /** Nombre de la cookie. */
    public static final String COOKIE_NAME = "librolog_refresh";

    /** Ruta a la que se envía la cookie. */
    public static final String COOKIE_PATH = "/api/auth";

    /** Cabecera que deben enviar las peticiones que usan la cookie. */
    public static final String CLIENT_HEADER = "X-LibroLog-Client";

    /** Valor esperado de la cabecera de cliente. */
    public static final String CLIENT_HEADER_VALUE = "web";

    private RefreshTokenCookies() {
    }

    /**
     * Crea la cookie con un refresh token.
     *
     * @param rawToken      token en claro
     * @param days          días de validez
     * @param secure        verdadero si la cookie debe viajar solo por HTTPS
     * @return cookie lista para añadir a la respuesta
     */
    public static ResponseCookie create(String rawToken, int days, boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofDays(days))
                .build();
    }

    /**
     * Crea la cookie vacía que borra la sesión en el navegador.
     *
     * @param secure verdadero si la cookie debe viajar solo por HTTPS
     * @return cookie caducada
     */
    public static ResponseCookie clear(boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
