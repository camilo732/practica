package com.librolog.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del límite de peticiones de autenticación.
 */
class RateLimitFilterTest {

    private MockHttpServletRequest peticion(String uri) {
        return peticion("POST", uri);
    }

    private MockHttpServletRequest peticion(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        request.setRemoteAddr("192.0.2.10");
        return request;
    }

    /**
     * Comprueba que se devuelve 429 al superar el cupo.
     *
     * @throws Exception si falla la cadena de filtros
     */
    @Test
    void devuelve429AlSuperarElCupo() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(3);

        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(peticion("/api/auth/login"), response, new MockFilterChain());
            assertNotEquals(429, response.getStatus());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(peticion("/api/auth/login"), response, new MockFilterChain());

        assertEquals(429, response.getStatus());
        assertNotNull(response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("Demasiadas solicitudes"));
    }

    /**
     * Comprueba que las rutas públicas no consumen el cupo.
     *
     * @throws Exception si falla la cadena de filtros
     */
    @Test
    void noLimitaRutasAjenasAlLogin() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1);

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(peticion("/api/public/info"), response, new MockFilterChain());
            assertNotEquals(429, response.getStatus());
        }
    }

    /**
     * Comprueba que la renovación de sesión tiene un cupo mayor y separado del
     * de las credenciales: cada página del frontend renueva su token al cargar.
     *
     * @throws Exception si falla la cadena de filtros
     */
    @Test
    void laRenovacionDeSesionTieneCupoPropioYMayor() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1);

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(peticion("/api/auth/refresh"), response, new MockFilterChain());
            assertNotEquals(429, response.getStatus());
        }

        MockHttpServletResponse login = new MockHttpServletResponse();
        filter.doFilter(peticion("/api/auth/login"), login, new MockFilterChain());
        assertNotEquals(429, login.getStatus());
    }

    /**
     * Comprueba que la consulta del perfil no consume cupo de autenticación.
     *
     * @throws Exception si falla la cadena de filtros
     */
    @Test
    void elPerfilNoConsumeCupoDeAutenticacion() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1);

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(peticion("GET", "/api/auth/me"), response, new MockFilterChain());
            assertNotEquals(429, response.getStatus());
        }
    }
}
