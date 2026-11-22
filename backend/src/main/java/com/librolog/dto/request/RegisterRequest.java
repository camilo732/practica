package com.librolog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de registro de un usuario local.
 *
 * @param username nombre de usuario público
 * @param email    correo electrónico
 * @param password contraseña en claro; se cifra con BCrypt antes de guardarla
 * @param fullName nombre completo, opcional
 * @param language idioma preferido: {@code es}, {@code en} o {@code de}
 */
@Schema(description = "Datos para crear una cuenta local")
public record RegisterRequest(

        @Schema(description = "Nombre de usuario", example = "ana_lectora")
        @NotBlank(message = "El nombre de usuario es obligatorio.")
        @Size(min = 3, max = 30, message = "El nombre de usuario debe tener entre 3 y 30 caracteres.")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Solo se permiten letras, números, punto, guion y guion bajo.")
        String username,

        @Schema(description = "Correo electrónico", example = "ana@ejemplo.com")
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 255, message = "El correo no puede superar 255 caracteres.")
        String email,

        @Schema(description = "Contraseña de al menos 10 caracteres con letras y números",
                example = "Lectura2026")
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 10, max = 72, message = "La contraseña debe tener entre 10 y 72 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contraseña debe incluir al menos una letra y un número.")
        String password,

        @Schema(description = "Nombre completo", example = "Ana Pérez")
        @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres.")
        String fullName,

        @Schema(description = "Idioma preferido", example = "es", allowableValues = {"es", "en", "de"})
        @Pattern(regexp = "^(es|en|de)$", message = "El idioma debe ser es, en o de.")
        String language) {
}
