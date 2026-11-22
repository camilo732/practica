package com.librolog.dto.response;

import com.librolog.entity.ReadingGoal;
import com.librolog.entity.enums.GoalPeriod;
import com.librolog.service.GoalPaceCalculator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Meta de lectura con su avance y su ritmo calculado.
 *
 * <p>Los campos de avance se calculan en el momento de la consulta: no se
 * guardan en la base, así que nunca quedan desactualizados.</p>
 *
 * @param id                    identificador de la meta
 * @param period                periodicidad
 * @param year                  año
 * @param month                 mes, nulo en metas anuales
 * @param targetBooks           libros objetivo
 * @param targetPages           páginas objetivo
 * @param periodStart           primer día del periodo
 * @param periodEnd             último día del periodo
 * @param daysTotal             días del periodo
 * @param daysElapsed           días transcurridos
 * @param daysRemaining         días restantes
 * @param booksRead             libros terminados en el periodo
 * @param pagesRead             páginas leídas en el periodo
 * @param booksPercent          porcentaje de libros
 * @param pagesPercent          porcentaje de páginas
 * @param expectedPercent       porcentaje esperado a la fecha
 * @param onTrack               verdadero si va al ritmo previsto
 * @param expectedBooks         libros esperados a la fecha
 * @param booksRemaining        libros que faltan
 * @param pagesRemaining        páginas que faltan
 * @param requiredBooksPerMonth ritmo necesario de libros por mes
 * @param requiredPagesPerDay   ritmo necesario de páginas por día
 * @param createdAt             fecha de creación
 * @param updatedAt             última modificación
 */
@Schema(description = "Meta de lectura con avance y ritmo calculado")
public record ReadingGoalResponse(

        @Schema(description = "Identificador", example = "3")
        Long id,

        @Schema(description = "Periodicidad", example = "ANNUAL")
        GoalPeriod period,

        @Schema(description = "Año", example = "2026")
        int year,

        @Schema(description = "Mes (nulo en anuales)", example = "3")
        Integer month,

        @Schema(description = "Libros objetivo", example = "24")
        int targetBooks,

        @Schema(description = "Páginas objetivo", example = "8000")
        int targetPages,

        @Schema(description = "Primer día del periodo", example = "2026-01-01")
        LocalDate periodStart,

        @Schema(description = "Último día del periodo", example = "2026-12-31")
        LocalDate periodEnd,

        @Schema(description = "Días del periodo", example = "365")
        int daysTotal,

        @Schema(description = "Días transcurridos", example = "323")
        int daysElapsed,

        @Schema(description = "Días restantes", example = "42")
        int daysRemaining,

        @Schema(description = "Libros terminados", example = "15")
        long booksRead,

        @Schema(description = "Páginas leídas", example = "5100")
        long pagesRead,

        @Schema(description = "Porcentaje de libros", example = "63")
        int booksPercent,

        @Schema(description = "Porcentaje de páginas", example = "64")
        int pagesPercent,

        @Schema(description = "Porcentaje esperado a la fecha", example = "88")
        int expectedPercent,

        @Schema(description = "Va al ritmo previsto", example = "false")
        boolean onTrack,

        @Schema(description = "Libros esperados a la fecha", example = "21.2")
        double expectedBooks,

        @Schema(description = "Libros que faltan", example = "9")
        long booksRemaining,

        @Schema(description = "Páginas que faltan", example = "2900")
        long pagesRemaining,

        @Schema(description = "Libros necesarios por mes", example = "6.5")
        double requiredBooksPerMonth,

        @Schema(description = "Páginas necesarias por día", example = "69")
        double requiredPagesPerDay,

        @Schema(description = "Fecha de creación")
        Instant createdAt,

        @Schema(description = "Última modificación")
        Instant updatedAt) {

    /**
     * Combina la meta con el resultado del cálculo de ritmo.
     *
     * @param goal meta de lectura
     * @param pace avance y ritmo calculados
     * @return respuesta lista para serializar
     */
    public static ReadingGoalResponse from(ReadingGoal goal, GoalPaceCalculator.Pace pace) {
        return new ReadingGoalResponse(
                goal.getId(),
                goal.getPeriod(),
                goal.getYear(),
                goal.getMonth() == null ? null : goal.getMonth().intValue(),
                goal.getTargetBooks(),
                goal.getTargetPages(),
                pace.periodStart(),
                pace.periodEnd(),
                pace.daysTotal(),
                pace.daysElapsed(),
                pace.daysRemaining(),
                pace.booksRead(),
                pace.pagesRead(),
                pace.booksPercent(),
                pace.pagesPercent(),
                pace.expectedPercent(),
                pace.onTrack(),
                pace.expectedBooks(),
                pace.booksRemaining(),
                pace.pagesRemaining(),
                pace.requiredBooksPerMonth(),
                pace.requiredPagesPerDay(),
                goal.getCreatedAt(),
                goal.getUpdatedAt());
    }
}
