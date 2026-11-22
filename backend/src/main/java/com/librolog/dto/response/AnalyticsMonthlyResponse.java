package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Serie mensual de actividad de un año.
 *
 * <p>Es la representación que consume un gráfico de líneas o de barras: siempre
 * incluye los doce meses, aunque estén a cero.</p>
 *
 * @param year   año consultado
 * @param months actividad de enero a diciembre
 */
@Schema(description = "Actividad mensual de un año")
public record AnalyticsMonthlyResponse(

        @Schema(description = "Año consultado", example = "2026")
        int year,

        @Schema(description = "Doce meses, de enero a diciembre")
        List<MonthlyActivityResponse> months) {
}
