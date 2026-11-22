package com.librolog.dto.response;

import com.librolog.entity.UserBook;
import com.librolog.entity.enums.BookStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entrada de la biblioteca personal.
 *
 * @param id              identificador de la entrada
 * @param book            datos del libro
 * @param status          estado de lectura
 * @param currentPage     página actual
 * @param totalPages      total de páginas de la edición
 * @param progressPercent porcentaje leído de 0 a 100
 * @param startedAt       fecha de inicio
 * @param finishedAt      fecha de fin
 * @param createdAt       fecha de alta en la biblioteca
 * @param updatedAt       última modificación
 */
@Schema(description = "Libro de la biblioteca personal")
public record UserBookResponse(

        @Schema(description = "Identificador de la entrada", example = "5")
        Long id,

        @Schema(description = "Libro")
        BookResponse book,

        @Schema(description = "Estado de lectura", example = "READING")
        BookStatus status,

        @Schema(description = "Página actual", example = "120")
        int currentPage,

        @Schema(description = "Total de páginas", example = "350")
        int totalPages,

        @Schema(description = "Porcentaje leído", example = "34")
        int progressPercent,

        @Schema(description = "Fecha de inicio")
        LocalDate startedAt,

        @Schema(description = "Fecha de fin")
        LocalDate finishedAt,

        @Schema(description = "Fecha de alta")
        Instant createdAt,

        @Schema(description = "Última modificación")
        Instant updatedAt) {

    /**
     * Convierte una entidad en su representación pública.
     *
     * @param entry entrada de biblioteca
     * @return datos públicos
     */
    public static UserBookResponse from(UserBook entry) {
        return new UserBookResponse(
                entry.getId(),
                BookResponse.from(entry.getBook()),
                entry.getStatus(),
                entry.getCurrentPage(),
                entry.getTotalPages(),
                percent(entry.getCurrentPage(), entry.getTotalPages()),
                entry.getStartedAt(),
                entry.getFinishedAt(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }

    /**
     * Calcula el porcentaje leído.
     *
     * <p>Si el total es desconocido (0) el porcentaje es 0. El resultado se
     * limita al rango 0–100 para que un dato inconsistente no rompa la vista.</p>
     *
     * @param currentPage página actual
     * @param totalPages  total de páginas
     * @return porcentaje entero entre 0 y 100
     */
    public static int percent(int currentPage, int totalPages) {
        if (totalPages <= 0) {
            return 0;
        }
        int value = (int) Math.round(currentPage * 100.0 / totalPages);
        return Math.max(0, Math.min(100, value));
    }
}
