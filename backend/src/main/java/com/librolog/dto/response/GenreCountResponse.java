package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Libros terminados de un género.
 *
 * @param genre género
 * @param books libros terminados
 */
@Schema(description = "Género y número de libros terminados")
public record GenreCountResponse(

        @Schema(description = "Género", example = "Fantasy")
        String genre,

        @Schema(description = "Libros terminados", example = "3")
        long books) {
}
