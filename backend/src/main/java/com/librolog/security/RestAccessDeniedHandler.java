package com.librolog.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 403 en JSON cuando el usuario autenticado no tiene permiso.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Escribe la respuesta 403.
     *
     * @param request               petición HTTP
     * @param response              respuesta HTTP
     * @param accessDeniedException causa del rechazo
     * @throws IOException si no se puede escribir el cuerpo
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ProblemJsonWriter.write(response, HttpStatus.FORBIDDEN.value(),
                "Acceso denegado",
                "No tienes permiso para realizar esta acción.");
    }
}
