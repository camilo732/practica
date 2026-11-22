package com.librolog.service;

import com.librolog.dto.response.AuthorCountResponse;
import com.librolog.dto.response.MonthlyActivityResponse;
import com.librolog.dto.response.RatingCountResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de los cálculos de estadísticas.
 */
class AnalyticsCalculatorTest {

    /**
     * Comprueba que la serie mensual separa libros y páginas por mes y año.
     */
    @Test
    void agrupaLibrosYPaginasPorMes() {
        List<AnalyticsCalculator.FinishedBook> finished = List.of(
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 1, 15), "Autora A"),
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 1, 20), "Autora B"),
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 3, 2), "Autora A"),
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2025, 1, 10), "Autora A"));

        List<AnalyticsCalculator.PageLog> logs = List.of(
                new AnalyticsCalculator.PageLog(LocalDate.of(2026, 1, 5), 40),
                new AnalyticsCalculator.PageLog(LocalDate.of(2026, 1, 6), 60),
                new AnalyticsCalculator.PageLog(LocalDate.of(2026, 3, 2), 30),
                new AnalyticsCalculator.PageLog(LocalDate.of(2025, 3, 2), 999));

        List<MonthlyActivityResponse> monthly =
                AnalyticsCalculator.monthly(2026, finished, logs);

        assertEquals(12, monthly.size());
        assertEquals(2, monthly.get(0).booksFinished());
        assertEquals(100, monthly.get(0).pagesRead());
        assertEquals(1, monthly.get(2).booksFinished());
        assertEquals(30, monthly.get(2).pagesRead());
        assertEquals(0, monthly.get(11).booksFinished());
    }

    /**
     * Comprueba que la serie mensual siempre tiene doce elementos.
     */
    @Test
    void devuelveDoceMesesSinDatos() {
        List<MonthlyActivityResponse> monthly =
                AnalyticsCalculator.monthly(2026, List.of(), List.of());

        assertEquals(12, monthly.size());
        assertTrue(monthly.stream()
                .allMatch(month -> month.booksFinished() == 0 && month.pagesRead() == 0));
    }

    /**
     * Comprueba la media con un decimal y el descarte de valores inválidos.
     */
    @Test
    void calculaLaCalificacionMedia() {
        assertEquals(4.3, AnalyticsCalculator.averageRating(
                java.util.Arrays.asList(5, 4, 4, null, 0, 6)));
        assertEquals(5.0, AnalyticsCalculator.averageRating(List.of(5)));
        assertEquals(0.0, AnalyticsCalculator.averageRating(List.of()));
        assertEquals(0.0, AnalyticsCalculator.averageRating(List.of(0, 7)));
    }

    /**
     * Comprueba que la distribución incluye siempre las cinco estrellas.
     */
    @Test
    void calculaLaDistribucionDeCalificaciones() {
        List<RatingCountResponse> distribution =
                AnalyticsCalculator.ratingDistribution(
                        java.util.Arrays.asList(5, 5, 4, 3, 3, 3, null));

        assertEquals(5, distribution.size());
        assertEquals(1, distribution.get(0).rating());
        assertEquals(0, distribution.get(0).count());
        assertEquals(2, distribution.get(4).count());
        assertEquals(1, distribution.get(3).count());
        assertEquals(3, distribution.get(2).count());
    }

    /**
     * Comprueba el agrupado de autorías sin distinguir mayúsculas.
     */
    @Test
    void agrupaAutoresSinDistinguirMayusculas() {
        List<AnalyticsCalculator.FinishedBook> finished = List.of(
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 1, 1), "Ursula K. Le Guin"),
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 2, 1), "ursula k. le guin"),
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 3, 1), "Octavia Butler"),
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 4, 1), "  "),
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 5, 1), null));

        List<AuthorCountResponse> authors =
                AnalyticsCalculator.topAuthors(finished, 5);

        assertEquals(2, authors.size());
        assertEquals("Ursula K. Le Guin", authors.get(0).author());
        assertEquals(2, authors.get(0).books());
        assertEquals("Octavia Butler", authors.get(1).author());
    }

    /**
     * Comprueba el límite de autorías devueltas.
     */
    @Test
    void respetaElLimiteDeAutores() {
        List<AnalyticsCalculator.FinishedBook> finished = List.of(
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 1, 1), "A"),
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 1, 2), "B"),
                new AnalyticsCalculator.FinishedBook(LocalDate.of(2026, 1, 3), "C"));

        assertEquals(2, AnalyticsCalculator.topAuthors(finished, 2).size());
        assertTrue(AnalyticsCalculator.topAuthors(finished, 0).isEmpty());
    }

    /**
     * Comprueba el mes con más páginas y el empate.
     */
    @Test
    void encuentraElMesConMasPaginas() {
        List<MonthlyActivityResponse> monthly = List.of(
                new MonthlyActivityResponse(1, 0, 100),
                new MonthlyActivityResponse(2, 0, 300),
                new MonthlyActivityResponse(3, 0, 300));

        assertEquals(2, AnalyticsCalculator.busiestMonth(monthly));
        assertEquals(0, AnalyticsCalculator.busiestMonth(List.of(
                new MonthlyActivityResponse(1, 2, 0))));
        assertEquals(0, AnalyticsCalculator.busiestMonth(List.of()));
    }
}
