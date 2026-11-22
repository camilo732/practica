package com.librolog.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escribe respuestas de error en formato {@code application/problem+json}.
 *
 * <p>Se usa en los filtros de seguridad, donde todavía no interviene el
 * manejador global de excepciones de Spring MVC.</p>
 */
public final class ProblemJsonWriter {

    private ProblemJsonWriter() {
    }

    /**
     * Escribe una respuesta de problema con estado, título y detalle.
     *
     * @param response respuesta HTTP
     * @param status   código de estado
     * @param title    título breve
     * @param detail   explicación para la persona usuaria
     * @throws IOException si no se puede escribir el cuerpo
     */
    public static void write(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s"}"""
                .formatted(title, status, detail));
    }
}
