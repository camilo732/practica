package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumen de la biblioteca personal por estado.
 *
 * @param total     total de libros
 * @param wantToRead pendientes de empezar
 * @param reading   en lectura
 * @param read      terminados
 */
@Schema(description = "Resumen de la biblioteca")
public record LibraryStatsResponse(

        @Schema(description = "Total de libros", example = "18")
        long total,

        @Schema(description = "Pendientes", example = "6")
        long wantToRead,

        @Schema(description = "En lectura", example = "3")
        long reading,

        @Schema(description = "Terminados", example = "9")
        long read) {
}
