package com.librolog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cambio del rol de administrador de una cuenta.
 *
 * @param admin {@code true} para conceder el rol ADMIN, {@code false} para
 *              retirarlo
 */
@Schema(description = "Cambio del rol de administrador")
public record UpdateUserRoleRequest(

        @Schema(description = "Conceder el rol ADMIN", example = "true")
        @NotNull(message = "Indica si la cuenta debe ser administradora.")
        Boolean admin) {
}
