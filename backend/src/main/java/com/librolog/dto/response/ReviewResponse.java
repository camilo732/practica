package com.librolog.dto.response;

import com.librolog.entity.Review;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Reseña estructurada devuelta por la API.
 *
 * @param id               identificador de la reseña
 * @param userBookId       entrada de biblioteca reseñada
 * @param book             libro reseñado
 * @param rating           calificación general de 1 a 5
 * @param paceRating       ritmo de 1 a 5
 * @param charactersRating personajes de 1 a 5
 * @param usefulnessRating utilidad de 1 a 5
 * @param content          texto de la reseña
 * @param createdAt        fecha de creación
 * @param updatedAt        última modificación
 */
@Schema(description = "Reseña de un libro")
public record ReviewResponse(

        @Schema(description = "Identificador de la reseña", example = "8")
        Long id,

        @Schema(description = "Entrada de biblioteca reseñada", example = "5")
        Long userBookId,

        @Schema(description = "Libro reseñado")
        BookResponse book,

        @Schema(description = "Calificación general", example = "5")
        int rating,

        @Schema(description = "Ritmo", example = "4")
        Integer paceRating,

        @Schema(description = "Personajes", example = "5")
        Integer charactersRating,

        @Schema(description = "Utilidad", example = "3")
        Integer usefulnessRating,

        @Schema(description = "Texto de la reseña")
        String content,

        @Schema(description = "Fecha de creación")
        Instant createdAt,

        @Schema(description = "Última modificación")
        Instant updatedAt) {

    /**
     * Convierte una entidad en su representación pública.
     *
     * @param review reseña con la entrada y el libro cargados
     * @return datos públicos de la reseña
     */
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUserBook().getId(),
                BookResponse.from(review.getUserBook().getBook()),
                review.getRating(),
                review.getPaceRating() == null ? null : review.getPaceRating().intValue(),
                review.getCharactersRating() == null ? null : review.getCharactersRating().intValue(),
                review.getUsefulnessRating() == null ? null : review.getUsefulnessRating().intValue(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
