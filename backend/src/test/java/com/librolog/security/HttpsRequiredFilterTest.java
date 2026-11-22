package com.librolog.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del filtro que exige HTTPS.
 */
class HttpsRequiredFilterTest {

    private MockHttpServletRequest peticion(boolean segura) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/info");
        request.setRequestURI("/api/public/info");
        request.setSecure(segura);
        return request;
    }

    /**
     * Comprueba que una petición sin cifrar se rechaza con 403.
     *
     * @throws Exception si falla la cadena de filtros
     */
    @Test
    void rechazaPeticionSinCifrar() throws Exception {
        HttpsRequiredFilter filter = new HttpsRequiredFilter(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(peticion(false), response, new MockFilterChain());

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("HTTPS requerido"));
    }

    /**
     * Comprueba que una petición cifrada continúa la cadena.
     *
     * @throws Exception si falla la cadena de filtros
     */
    @Test
    void permitePeticionCifrada() throws Exception {
        HttpsRequiredFilter filter = new HttpsRequiredFilter(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(peticion(true), response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    /**
     * Comprueba que el filtro es transparente cuando está desactivado.
     *
     * @throws Exception si falla la cadena de filtros
     */
    @Test
    void noIntervieneSiEstaDesactivado() throws Exception {
        HttpsRequiredFilter filter = new HttpsRequiredFilter(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(peticion(false), response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }
}
