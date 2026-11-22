package com.librolog.service;

import com.librolog.entity.enums.GoalPeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del cálculo de ritmo de las metas.
 *
 * <p>Se usan fechas fijas para que el resultado no dependa del día en que se
 * ejecuten las pruebas.</p>
 */
class GoalPaceCalculatorTest {

    /**
     * Comprueba una meta anual a mitad de año.
     */
    @Test
    void calculaUnaMetaAnualAMitadDeAnio() {
        GoalPaceCalculator.Pace pace = GoalPaceCalculator.calculate(
                GoalPeriod.ANNUAL, 2025, null,
                12, 3600,
                6, 1800,
                LocalDate.of(2025, 7, 2));

        assertEquals(365, pace.daysTotal());
        assertEquals(183, pace.daysElapsed());
        assertEquals(183, pace.daysRemaining());
        assertEquals(50, pace.expectedPercent());
        assertEquals(50, pace.booksPercent());
        assertEquals(50, pace.pagesPercent());
        assertTrue(pace.onTrack());
        assertEquals(6.0, pace.expectedBooks());
        assertEquals(6, pace.booksRemaining());
        assertEquals(1800, pace.pagesRemaining());
        assertEquals(1.0, pace.requiredBooksPerMonth());
        assertEquals(9.8, pace.requiredPagesPerDay());
        assertEquals(LocalDate.of(2025, 1, 1), pace.periodStart());
        assertEquals(LocalDate.of(2025, 12, 31), pace.periodEnd());
    }

    /**
     * Comprueba que ir por debajo del ritmo se detecta.
     */
    @Test
    void detectaQueVaPorDebajoDelRitmo() {
        GoalPaceCalculator.Pace pace = GoalPaceCalculator.calculate(
                GoalPeriod.ANNUAL, 2025, null,
                20, 6000,
                5, 1500,
                LocalDate.of(2025, 10, 1));

        assertEquals(75, pace.expectedPercent());
        assertEquals(25, pace.booksPercent());
        assertFalse(pace.onTrack());
        assertEquals(15, pace.booksRemaining());
        assertTrue(pace.requiredPagesPerDay() > 0);
    }

    /**
     * Comprueba una meta mensual con el mes completo.
     */
    @Test
    void calculaUnaMetaMensual() {
        GoalPaceCalculator.Pace pace = GoalPaceCalculator.calculate(
                GoalPeriod.MONTHLY, 2026, 1,
                2, 600,
                1, 300,
                LocalDate.of(2026, 1, 15));

        assertEquals(31, pace.daysTotal());
        assertEquals(15, pace.daysElapsed());
        assertEquals(17, pace.daysRemaining());
        assertEquals(48, pace.expectedPercent());
        assertEquals(50, pace.booksPercent());
        assertTrue(pace.onTrack());
        assertEquals(1.8, pace.requiredBooksPerMonth());
        assertEquals(17.6, pace.requiredPagesPerDay());
    }

    /**
     * Comprueba que una meta futura no se considera incumplida.
     */
    @Test
    void unaMetaFuturaEmpiezaSinAvance() {
        GoalPaceCalculator.Pace pace = GoalPaceCalculator.calculate(
                GoalPeriod.ANNUAL, 2027, null,
                12, 4000,
                0, 0,
                LocalDate.of(2026, 11, 19));

        assertEquals(0, pace.daysElapsed());
        assertEquals(365, pace.daysRemaining());
        assertEquals(0, pace.expectedPercent());
        assertEquals(0.0, pace.expectedBooks());
        assertTrue(pace.onTrack());
    }

    /**
     * Comprueba una meta pasada y cumplida.
     */
    @Test
    void unaMetaPasadaCumplidaEstaEnRitmo() {
        GoalPaceCalculator.Pace pace = GoalPaceCalculator.calculate(
                GoalPeriod.ANNUAL, 2024, null,
                10, 3000,
                11, 3300,
                LocalDate.of(2026, 11, 19));

        assertEquals(366, pace.daysTotal());
        assertEquals(366, pace.daysElapsed());
        assertEquals(0, pace.daysRemaining());
        assertEquals(100, pace.expectedPercent());
        assertEquals(100, pace.booksPercent());
        assertTrue(pace.onTrack());
        assertEquals(0, pace.booksRemaining());
        assertEquals(0.0, pace.requiredBooksPerMonth());
    }

    /**
     * Comprueba una meta pasada y no cumplida: el ritmo necesario es todo lo
     * que falta, porque ya no quedan días.
     */
    @Test
    void unaMetaPasadaIncumplidaExigeLoQueFalta() {
        GoalPaceCalculator.Pace pace = GoalPaceCalculator.calculate(
                GoalPeriod.ANNUAL, 2024, null,
                10, 3000,
                9, 2700,
                LocalDate.of(2026, 11, 19));

        assertEquals(100, pace.expectedPercent());
        assertEquals(90, pace.booksPercent());
        assertFalse(pace.onTrack());
        assertEquals(1, pace.booksRemaining());
        assertEquals(1.0, pace.requiredBooksPerMonth());
        assertEquals(300.0, pace.requiredPagesPerDay());
    }

    /**
     * Comprueba que el porcentaje se limita a 100 al superar el objetivo.
     */
    @Test
    void limitaElPorcentajeAlSuperarElObjetivo() {
        GoalPaceCalculator.Pace pace = GoalPaceCalculator.calculate(
                GoalPeriod.ANNUAL, 2025, null,
                12, 3000,
                20, 5000,
                LocalDate.of(2025, 12, 31));

        assertEquals(100, pace.booksPercent());
        assertEquals(100, pace.pagesPercent());
        assertEquals(0, pace.booksRemaining());
        assertEquals(0, pace.pagesRemaining());
    }

    /**
     * Comprueba el último día de un febrero bisiesto.
     */
    @Test
    void tieneEnCuentaElAnioBisiesto() {
        GoalPaceCalculator.Bounds bounds =
                GoalPaceCalculator.bounds(GoalPeriod.MONTHLY, 2024, 2);

        assertEquals(LocalDate.of(2024, 2, 1), bounds.start());
        assertEquals(LocalDate.of(2024, 2, 29), bounds.end());

        GoalPaceCalculator.Pace pace = GoalPaceCalculator.calculate(
                GoalPeriod.MONTHLY, 2024, 2,
                4, 1000,
                0, 0,
                LocalDate.of(2024, 2, 29));

        assertEquals(29, pace.daysTotal());
        assertEquals(29, pace.daysElapsed());
        assertEquals(100, pace.expectedPercent());
        assertFalse(pace.onTrack());
    }

    /**
     * Comprueba que una meta mensual sin mes es un error de programación.
     */
    @Test
    void rechazaUnaMetaMensualSinMes() {
        assertThrows(IllegalArgumentException.class,
                () -> GoalPaceCalculator.bounds(GoalPeriod.MONTHLY, 2026, null));
    }
}
