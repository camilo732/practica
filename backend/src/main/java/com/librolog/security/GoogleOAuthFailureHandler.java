package com.librolog.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Devuelve al formulario de acceso cuando Google no completa la autenticación.
 *
 * <p>No se muestra el motivo real: puede contener detalles del proveedor o de
 * la cuenta. El registro del servidor conserva la causa para diagnóstico.</p>
 */
@Component
public class GoogleOAuthFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthFailureHandler.class);

    /**
     * Redirige al acceso con un aviso genérico.
     *
     * @param request   petición HTTP
     * @param response  respuesta HTTP
     * @param exception causa del fallo
     * @throws IOException      si falla la redirección
     * @throws ServletException si falla el manejo de la respuesta
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        log.warn("Fallo de autenticación con Google: {}", exception.getMessage());
        response.sendRedirect("/login.html?oauth=error");
    }
}
