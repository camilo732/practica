package com.librolog.dto.response;

import com.librolog.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Datos de una cuenta vistos por un administrador.
 *
 * <p>Nunca incluye la contraseña ni su hash. Añade información operativa que no
 * aparece en el perfil público: proveedor, intentos fallidos y bloqueo.</p>
 *
 * @param id             identificador
 * @param username       nombre de usuario
 * @param email          correo electrónico
 * @param fullName       nombre completo
 * @param language       idioma preferido
 * @param enabled        estado de la cuenta
 * @param authProvider   proveedor de autenticación
 * @param roles          roles asignados
 * @param failedAttempts intentos fallidos consecutivos
 * @param lockedUntil    fin del bloqueo temporal, o {@code null}
 * @param locked         verdadero si el bloqueo sigue activo
 * @param createdAt      fecha de registro
 * @param updatedAt      fecha de la última modificación
 */
@Schema(description = "Cuenta vista por un administrador")
public record AdminUserResponse(

        @Schema(description = "Identificador", example = "4")
        Long id,

        @Schema(description = "Nombre de usuario", example = "ana_lectora")
        String username,

        @Schema(description = "Correo electrónico", example = "ana@ejemplo.com")
        String email,

        @Schema(description = "Nombre completo", example = "Ana Pérez")
        String fullName,

        @Schema(description = "Idioma preferido", example = "es")
        String language,

        @Schema(description = "Cuenta habilitada", example = "true")
        boolean enabled,

        @Schema(description = "Proveedor de autenticación", example = "LOCAL")
        String authProvider,

        @Schema(description = "Roles", example = "[\"USER\"]")
        List<String> roles,

        @Schema(description = "Intentos fallidos consecutivos", example = "0")
        int failedAttempts,

        @Schema(description = "Fin del bloqueo temporal")
        Instant lockedUntil,

        @Schema(description = "Bloqueo activo", example = "false")
        boolean locked,

        @Schema(description = "Fecha de registro")
        Instant createdAt,

        @Schema(description = "Última modificación")
        Instant updatedAt) {

    /**
     * Convierte una entidad en su representación administrativa.
     *
     * @param user entidad de usuario
     * @return datos administrativos
     */
    public static AdminUserResponse from(User user) {
        return from(user, Instant.now());
    }

    /**
     * Convierte una entidad en su representación administrativa con una fecha
     * de referencia.
     *
     * @param user entidad de usuario
     * @param now  fecha usada para decidir si el bloqueo sigue activo
     * @return datos administrativos
     */
    public static AdminUserResponse from(User user, Instant now) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();

        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);

        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getLanguage(),
                user.isEnabled(),
                user.getAuthProvider().name(),
                roles,
                user.getFailedAttempts(),
                user.getLockedUntil(),
                locked,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
