package com.librolog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Exige HTTPS cuando {@code app.security.require-https} está activo.
 *
 * <p>Se apoya en {@code X-Forwarded-Proto} para funcionar detrás de un proxy
 * inverso que termine el TLS.</p>
 */
public class HttpsRequiredFilter extends OncePerRequestFilter {

    private final boolean requireHttps;

    /**
     * Crea el filtro según la configuración.
     *
     * @param requireHttps verdadero para rechazar peticiones no cifradas
     */
    public HttpsRequiredFilter(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    /**
     * Rechaza la petición si llega sin cifrar.
     *
     * @param request  petición HTTP
     * @param response respuesta HTTP
     * @param chain    siguiente filtro
     * @throws ServletException si falla la cadena
     * @throws IOException      si falla la escritura
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!requireHttps) {
            chain.doFilter(request, response);
            return;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        boolean secure = request.isSecure() || "https".equalsIgnoreCase(forwardedProto);

        if (!secure) {
            ProblemJsonWriter.write(response, HttpStatus.FORBIDDEN.value(),
                    "HTTPS requerido",
                    "Esta instalación solo acepta peticiones cifradas con HTTPS.");
            return;
        }

        chain.doFilter(request, response);
    }
}
