package com.librolog.dto.request;

import com.librolog.entity.enums.GoalPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Nueva meta de lectura anual o mensual.
 *
 * <p>El mes debe ser {@code null} en las metas anuales y estar entre 1 y 12 en
 * las mensuales; el servicio lo comprueba además de la validación de campos.</p>
 *
 * @param period      periodicidad
 * @param year        año de la meta
 * @param month       mes de la meta, solo en las mensuales
 * @param targetBooks libros que se quieren leer
 * @param targetPages páginas que se quieren leer
 */
@Schema(description = "Meta de lectura")
public record ReadingGoalRequest(

        @Schema(description = "Periodicidad", example = "ANNUAL")
        @NotNull(message = "La periodicidad es obligatoria.")
        GoalPeriod period,

        @Schema(description = "Año", example = "2026")
        @NotNull(message = "El año es obligatorio.")
        @Min(value = 2000, message = "El año debe estar entre 2000 y 2100.")
        @Max(value = 2100, message = "El año debe estar entre 2000 y 2100.")
        Integer year,

        @Schema(description = "Mes (solo metas mensuales)", example = "3")
        @Min(value = 1, message = "El mes debe estar entre 1 y 12.")
        @Max(value = 12, message = "El mes debe estar entre 1 y 12.")
        Integer month,

        @Schema(description = "Libros objetivo", example = "24")
        @NotNull(message = "El objetivo de libros es obligatorio.")
        @Min(value = 1, message = "El objetivo de libros debe ser al menos 1.")
        Integer targetBooks,

        @Schema(description = "Páginas objetivo", example = "8000")
        @NotNull(message = "El objetivo de páginas es obligatorio.")
        @Min(value = 1, message = "El objetivo de páginas debe ser al menos 1.")
        Integer targetPages) {
}
