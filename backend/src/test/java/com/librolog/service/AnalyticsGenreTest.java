package com.librolog.service;

import com.librolog.dto.response.GenreCountResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsGenreTest {

    @Test
    void topGenresAgrupaSinDistinguirMayusculasEIgnoraVacios() {
        List<GenreCountResponse> result = AnalyticsCalculator.topGenres(
                Arrays.asList("Fantasy", "fantasy", "Science fiction", " ", null), 5);

        assertEquals(2, result.size());
        assertEquals("Fantasy", result.get(0).genre());
        assertEquals(2, result.get(0).books());
        assertEquals("Science fiction", result.get(1).genre());
    }

    @Test
    void topGenresRespetaElLimite() {
        assertEquals(1, AnalyticsCalculator.topGenres(List.of("A", "B", "C"), 1).size());
        assertTrue(AnalyticsCalculator.topGenres(null, 3).isEmpty());
    }

    @Test
    void averagePagesIgnoraNulosYCeros() {
        assertEquals(300.0, AnalyticsCalculator.averagePages(Arrays.asList(200, 400, null, 0)));
        assertEquals(0.0, AnalyticsCalculator.averagePages(List.of()));
        assertEquals(333.3, AnalyticsCalculator.averagePages(List.of(300, 300, 400)));
    }
}
