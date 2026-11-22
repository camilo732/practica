package com.librolog.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Cookie temporal que conserva la página de destino durante el viaje a Google.
 *
 * <p>La cookie es HttpOnly, dura cinco minutos y usa {@code SameSite=Lax}: el
 * regreso desde Google es una navegación de nivel superior y con
 * {@code Strict} el navegador no la enviaría. Nunca contiene tokens.</p>
 */
public final class OAuthNextCookie {

    /** Nombre de la cookie. */
    public static final String NAME = "librolog_oauth_next";

    private static final Duration MAX_AGE = Duration.ofMinutes(5);

    private OAuthNextCookie() {
    }

    /**
     * Crea la cookie con una ruta interna validada.
     *
     * @param next   ruta solicitada
     * @param secure verdadero si debe viajar solo por HTTPS
     * @return cookie, o {@code null} si la ruta no es segura
     */
    public static ResponseCookie create(String next, boolean secure) {
        String safe = safeNext(next);
        if (safe == null) {
            return null;
        }
        return ResponseCookie.from(NAME, safe)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(MAX_AGE)
                .build();
    }

    /**
     * Crea la cookie vacía que borra la ruta temporal.
     *
     * @param secure verdadero si debe viajar solo por HTTPS
     * @return cookie caducada
     */
    public static ResponseCookie clear(boolean secure) {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    /**
     * Lee la ruta de destino de la petición.
     *
     * @param request petición HTTP
     * @return ruta interna segura, o {@code null}
     */
    public static String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) {
                return safeNext(cookie.getValue());
            }
        }
        return null;
    }

    /**
     * Valida una ruta interna de continuación.
     *
     * <p>Solo se aceptan documentos HTML de la raíz de la aplicación, sin
     * páginas de acceso ni el propio retorno de OAuth.</p>
     *
     * @param value ruta solicitada
     * @return ruta segura, o {@code null}
     */
    public static String safeNext(String value) {
        if (value == null || value.isBlank() || value.length() > 512) {
            return null;
        }
        String path = value.trim();
        if (!path.startsWith("/") || path.startsWith("//")
                || path.contains("\\") || path.contains("\r") || path.contains("\n")) {
            return null;
        }
        if (!path.matches("^/[a-z-]+\\.html(\\?[^#]*)?$")) {
            return null;
        }
        if (path.matches("^/(login|register|oauth-callback)\\.html(\\?.*)?$")) {
            return null;
        }
        return path;
    }
}
