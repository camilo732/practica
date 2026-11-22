package com.librolog.dto.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas del cálculo del porcentaje de lectura.
 */
class UserBookResponseTest {

    /**
     * Comprueba el cálculo normal y el redondeo.
     */
    @Test
    void calculaElPorcentaje() {
        assertEquals(0, UserBookResponse.percent(0, 100));
        assertEquals(50, UserBookResponse.percent(50, 100));
        assertEquals(100, UserBookResponse.percent(100, 100));
        assertEquals(33, UserBookResponse.percent(1, 3));
        assertEquals(67, UserBookResponse.percent(2, 3));
    }

    /**
     * Comprueba que un total desconocido devuelve 0 y no divide entre cero.
     */
    @Test
    void evitaLaDivisionEntreCero() {
        assertEquals(0, UserBookResponse.percent(10, 0));
        assertEquals(0, UserBookResponse.percent(10, -5));
        assertEquals(0, UserBookResponse.percent(0, 0));
    }

    /**
     * Comprueba que un dato inconsistente se limita al rango 0–100.
     */
    @Test
    void limitaElResultadoAlRango() {
        assertEquals(100, UserBookResponse.percent(150, 100));
        assertEquals(0, UserBookResponse.percent(-10, 100));
    }
}
