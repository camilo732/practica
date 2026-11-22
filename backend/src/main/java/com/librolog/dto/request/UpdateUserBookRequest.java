package com.librolog.dto.request;

import com.librolog.entity.enums.BookStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

/**
 * Datos para modificar una entrada de la biblioteca.
 *
 * <p>Todos los campos son opcionales: {@code null} significa «no cambiar».</p>
 *
 * @param status     nuevo estado de lectura
 * @param totalPages total de páginas de la edición
 * @param startedAt  fecha de inicio, útil al importar datos de otro servicio
 */
@Schema(description = "Modificación de un libro de la biblioteca")
public record UpdateUserBookRequest(

        @Schema(description = "Estado de lectura", example = "READING")
        BookStatus status,

        @Schema(description = "Total de páginas", example = "350")
        @Min(value = 1, message = "El total de páginas debe ser al menos 1.")
        Integer totalPages,

        @Schema(description = "Fecha de inicio", example = "2026-01-15")
        LocalDate startedAt) {
}
