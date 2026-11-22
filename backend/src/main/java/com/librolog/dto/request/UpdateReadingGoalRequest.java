package com.librolog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Modificación de los objetivos de una meta.
 *
 * <p>La periodicidad, el año y el mes no cambian: una meta es única para su
 * periodo, así que modificarlos equivaldría a crear otra.</p>
 *
 * @param targetBooks nuevos libros objetivo
 * @param targetPages nuevas páginas objetivo
 */
@Schema(description = "Modificación de los objetivos de una meta")
public record UpdateReadingGoalRequest(

        @Schema(description = "Libros objetivo", example = "30")
        @NotNull(message = "El objetivo de libros es obligatorio.")
        @Min(value = 1, message = "El objetivo de libros debe ser al menos 1.")
        Integer targetBooks,

        @Schema(description = "Páginas objetivo", example = "9000")
        @NotNull(message = "El objetivo de páginas es obligatorio.")
        @Min(value = 1, message = "El objetivo de páginas debe ser al menos 1.")
        Integer targetPages) {
}
