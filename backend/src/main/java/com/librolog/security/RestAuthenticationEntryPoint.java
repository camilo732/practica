package com.librolog.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 401 en JSON cuando falta un token válido.
 *
 * <p>Sustituye a la redirección al formulario de login, que no aplica a una
 * API REST.</p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Escribe la respuesta 401.
     *
     * @param request       petición HTTP
     * @param response      respuesta HTTP
     * @param authException causa de la falta de autenticación
     * @throws IOException si no se puede escribir el cuerpo
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        ProblemJsonWriter.write(response, HttpStatus.UNAUTHORIZED.value(),
                "No autenticado",
                "Se requiere un token de acceso válido.");
    }
}
