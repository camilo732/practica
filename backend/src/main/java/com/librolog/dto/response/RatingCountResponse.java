package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Número de reseñas con una calificación concreta.
 *
 * @param rating calificación de 1 a 5
 * @param count  número de reseñas con esa calificación
 */
@Schema(description = "Cantidad de reseñas por calificación")
public record RatingCountResponse(

        @Schema(description = "Calificación (1-5)", example = "5")
        int rating,

        @Schema(description = "Reseñas con esa calificación", example = "7")
        long count) {
}
