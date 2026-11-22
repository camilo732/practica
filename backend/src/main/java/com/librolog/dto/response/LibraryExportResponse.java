package com.librolog.dto.response;

import com.librolog.entity.enums.BookStatus;
import com.librolog.entity.enums.GoalPeriod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Copia de seguridad completa de la biblioteca de un usuario.
 *
 * <p>Incluye libros, reseñas y metas en un solo documento JSON. No contiene
 * contraseñas, tokens ni datos de otras personas.</p>
 *
 * @param exportedAt fecha de exportación
 * @param username   nombre del usuario
 * @param totalBooks número de libros exportados
 * @param books      libros con su reseña
 * @param goals      metas de lectura
 */
@Schema(description = "Exportación completa de la biblioteca")
public record LibraryExportResponse(

        @Schema(description = "Fecha de exportación")
        Instant exportedAt,

        @Schema(description = "Usuario", example = "ana_lectora")
        String username,

        @Schema(description = "Libros exportados", example = "42")
        int totalBooks,

        @Schema(description = "Libros")
        List<BookEntry> books,

        @Schema(description = "Metas")
        List<GoalEntry> goals) {

    /**
     * Libro de la exportación.
     *
     * @param book            datos del libro
     * @param status          estado de lectura
     * @param currentPage     página actual
     * @param totalPages      total de páginas
     * @param progressPercent porcentaje leído
     * @param startedAt       fecha de inicio
     * @param finishedAt      fecha de fin
     * @param createdAt       fecha de alta
     * @param review          reseña, si existe
     */
    @Schema(description = "Libro exportado")
    public record BookEntry(
            BookResponse book,
            BookStatus status,
            int currentPage,
            int totalPages,
            int progressPercent,
            LocalDate startedAt,
            LocalDate finishedAt,
            Instant createdAt,
            ReviewEntry review) {
    }

    /**
     * Reseña de la exportación.
     *
     * @param rating           calificación general
     * @param paceRating       ritmo
     * @param charactersRating personajes
     * @param usefulnessRating utilidad
     * @param content          texto
     * @param createdAt        fecha de creación
     * @param updatedAt        última modificación
     */
    @Schema(description = "Reseña exportada")
    public record ReviewEntry(
            int rating,
            Integer paceRating,
            Integer charactersRating,
            Integer usefulnessRating,
            String content,
            Instant createdAt,
            Instant updatedAt) {
    }

    /**
     * Meta de la exportación.
     *
     * @param period      periodicidad
     * @param year        año
     * @param month       mes, nulo en las anuales
     * @param targetBooks libros objetivo
     * @param targetPages páginas objetivo
     */
    @Schema(description = "Meta exportada")
    public record GoalEntry(
            GoalPeriod period,
            int year,
            Integer month,
            int targetBooks,
            int targetPages) {
    }
}
