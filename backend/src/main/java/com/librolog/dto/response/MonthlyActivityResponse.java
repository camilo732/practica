package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Actividad de un mes concreto.
 *
 * @param month         mes, de 1 a 12
 * @param booksFinished libros terminados ese mes
 * @param pagesRead     páginas leídas ese mes
 */
@Schema(description = "Actividad de lectura de un mes")
public record MonthlyActivityResponse(

        @Schema(description = "Mes (1-12)", example = "11")
        int month,

        @Schema(description = "Libros terminados", example = "2")
        long booksFinished,

        @Schema(description = "Páginas leídas", example = "640")
        long pagesRead) {
}
