package com.librolog.dto.response;

import com.librolog.entity.AdminAuditLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Registro de auditoría administrativa.
 *
 * @param id             identificador
 * @param actorUsername  administrador que realizó la acción
 * @param targetUsername cuenta afectada
 * @param action         acción realizada
 * @param detail         detalle adicional
 * @param createdAt      fecha de la acción
 */
@Schema(description = "Acción administrativa auditada")
public record AdminAuditResponse(

        @Schema(description = "Identificador", example = "1")
        Long id,

        @Schema(description = "Administrador", example = "juans")
        String actorUsername,

        @Schema(description = "Cuenta afectada", example = "ana_lectora")
        String targetUsername,

        @Schema(description = "Acción", example = "USER_DISABLED")
        String action,

        @Schema(description = "Detalle", example = "Cuenta deshabilitada")
        String detail,

        @Schema(description = "Fecha de la acción")
        Instant createdAt) {

    /**
     * Convierte una entidad de auditoría en su DTO.
     *
     * @param log entidad
     * @return DTO
     */
    public static AdminAuditResponse from(AdminAuditLog log) {
        return new AdminAuditResponse(
                log.getId(),
                log.getActorUsername(),
                log.getTargetUsername(),
                log.getAction().name(),
                log.getDetail(),
                log.getCreatedAt());
    }
}
