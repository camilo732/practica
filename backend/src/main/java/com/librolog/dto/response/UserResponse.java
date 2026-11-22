package com.librolog.dto.response;

import com.librolog.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Datos públicos de un usuario. Nunca incluye la contraseña ni el hash.
 *
 * @param id        identificador
 * @param username  nombre de usuario
 * @param email     correo electrónico
 * @param fullName  nombre completo
 * @param language  idioma preferido
 * @param enabled   estado de la cuenta
 * @param roles     roles asignados
 * @param createdAt fecha de registro
 */
@Schema(description = "Datos públicos del usuario")
public record UserResponse(

        @Schema(description = "Identificador", example = "1")
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

        @Schema(description = "Roles", example = "[\"USER\"]")
        List<String> roles,

        @Schema(description = "Fecha de registro")
        Instant createdAt) {

    /**
     * Convierte una entidad en su representación pública.
     *
     * @param user entidad de usuario
     * @return datos públicos
     */
    public static UserResponse from(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getLanguage(),
                user.isEnabled(),
                roles,
                user.getCreatedAt());
    }
}
