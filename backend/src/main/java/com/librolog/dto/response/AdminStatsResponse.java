package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumen de cuentas para el panel de administración.
 *
 * @param totalUsers        total de cuentas
 * @param enabledUsers      cuentas habilitadas
 * @param disabledUsers     cuentas deshabilitadas
 * @param adminUsers        cuentas con rol ADMIN
 * @param lockedUsers       cuentas bloqueadas temporalmente
 * @param googleUsers       cuentas vinculadas a Google
 * @param localUsers        cuentas locales
 * @param newUsersThisMonth cuentas creadas en el mes actual
 */
@Schema(description = "Resumen de cuentas de LibroLog")
public record AdminStatsResponse(

        @Schema(description = "Total de cuentas", example = "24")
        long totalUsers,

        @Schema(description = "Cuentas habilitadas", example = "23")
        long enabledUsers,

        @Schema(description = "Cuentas deshabilitadas", example = "1")
        long disabledUsers,

        @Schema(description = "Cuentas administradoras", example = "2")
        long adminUsers,

        @Schema(description = "Cuentas bloqueadas", example = "0")
        long lockedUsers,

        @Schema(description = "Cuentas de Google", example = "5")
        long googleUsers,

        @Schema(description = "Cuentas locales", example = "19")
        long localUsers,

        @Schema(description = "Cuentas creadas este mes", example = "3")
        long newUsersThisMonth) {
}
