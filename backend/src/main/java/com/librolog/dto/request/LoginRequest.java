package com.librolog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de inicio de sesión.
 *
 * @param usernameOrEmail nombre de usuario o correo
 * @param password        contraseña en claro
 */
@Schema(description = "Credenciales de acceso")
public record LoginRequest(

        @Schema(description = "Nombre de usuario o correo", example = "ana_lectora")
        @NotBlank(message = "El usuario o correo es obligatorio.")
        @Size(max = 255, message = "El identificador no puede superar 255 caracteres.")
        String usernameOrEmail,

        @Schema(description = "Contraseña", example = "Lectura2026")
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 72, message = "La contraseña no puede superar 72 caracteres.")
        String password) {
}
