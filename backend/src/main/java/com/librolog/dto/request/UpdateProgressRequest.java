package com.librolog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para registrar el avance de lectura de un libro.
 *
 * @param currentPage página en la que se encuentra la persona lectora
 */
@Schema(description = "Actualización del progreso de lectura")
public record UpdateProgressRequest(

        @Schema(description = "Página actual", example = "120")
        @NotNull(message = "La página actual es obligatoria.")
        @Min(value = 0, message = "La página actual no puede ser negativa.")
        Integer currentPage) {
}
