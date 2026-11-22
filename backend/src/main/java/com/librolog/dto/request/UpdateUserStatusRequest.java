package com.librolog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cambio de estado de una cuenta desde el panel de administración.
 *
 * @param enabled {@code true} para habilitar la cuenta, {@code false} para
 *                deshabilitarla
 */
@Schema(description = "Cambio de estado de una cuenta")
public record UpdateUserStatusRequest(

        @Schema(description = "Cuenta habilitada", example = "false")
        @NotNull(message = "Indica si la cuenta debe estar habilitada.")
        Boolean enabled) {
}
