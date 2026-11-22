package com.librolog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reseña estructurada de un libro.
 *
 * <p>La calificación general es obligatoria. Las tres subcalificaciones son
 * opcionales: no todos los libros se valoran igual de bien por ritmo,
 * personajes o utilidad.</p>
 *
 * @param rating            calificación general de 1 a 5
 * @param paceRating        ritmo de 1 a 5
 * @param charactersRating  personajes de 1 a 5
 * @param usefulnessRating  utilidad de 1 a 5
 * @param content           texto libre de la reseña
 */
@Schema(description = "Reseña estructurada de un libro")
public record ReviewRequest(

        @Schema(description = "Calificación general (1-5)", example = "5")
        @NotNull(message = "La calificación general es obligatoria.")
        @Min(value = 1, message = "La calificación debe estar entre 1 y 5.")
        @Max(value = 5, message = "La calificación debe estar entre 1 y 5.")
        Integer rating,

        @Schema(description = "Ritmo (1-5)", example = "4")
        @Min(value = 1, message = "El ritmo debe estar entre 1 y 5.")
        @Max(value = 5, message = "El ritmo debe estar entre 1 y 5.")
        Integer paceRating,

        @Schema(description = "Personajes (1-5)", example = "5")
        @Min(value = 1, message = "Los personajes deben estar entre 1 y 5.")
        @Max(value = 5, message = "Los personajes deben estar entre 1 y 5.")
        Integer charactersRating,

        @Schema(description = "Utilidad (1-5)", example = "3")
        @Min(value = 1, message = "La utilidad debe estar entre 1 y 5.")
        @Max(value = 5, message = "La utilidad debe estar entre 1 y 5.")
        Integer usefulnessRating,

        @Schema(description = "Texto de la reseña", example = "Una novela que cambia la forma de leer.")
        @Size(max = 4000, message = "La reseña no puede superar 4000 caracteres.")
        String content) {
}
