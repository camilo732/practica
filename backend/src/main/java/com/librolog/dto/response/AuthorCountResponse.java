package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Libros terminados de una autoría.
 *
 * @param author autoría
 * @param books  libros terminados
 */
@Schema(description = "Autoría y número de libros terminados")
public record AuthorCountResponse(

        @Schema(description = "Autoría", example = "Ursula K. Le Guin")
        String author,

        @Schema(description = "Libros terminados", example = "4")
        long books) {
}
