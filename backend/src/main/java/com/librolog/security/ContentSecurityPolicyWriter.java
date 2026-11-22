package com.librolog.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;

/**
 * Añade la cabecera {@code Content-Security-Policy} salvo en Swagger UI.
 *
 * <p>Swagger UI inicializa la interfaz con un script en línea, así que una CSP
 * estricta rompería la documentación. El resto de la aplicación, incluidas las
 * páginas HTML, sí recibe la política.</p>
 */
public class ContentSecurityPolicyWriter implements HeaderWriter {

    private static final String HEADER = "Content-Security-Policy";

    private final String policy;

    /**
     * Crea el escritor con la política indicada.
     *
     * @param policy directivas CSP
     */
    public ContentSecurityPolicyWriter(String policy) {
        this.policy = policy;
    }

    /**
     * Escribe la cabecera si la ruta no es de documentación.
     *
     * @param request  petición HTTP
     * @param response respuesta HTTP
     */
    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        boolean documentacion = uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.equals("/swagger-ui.html");
        if (!documentacion) {
            response.setHeader(HEADER, policy);
        }
    }
}
