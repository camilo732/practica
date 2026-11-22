package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de autenticación.
 *
 * <p>El refresh token no aparece en el cuerpo: viaja en una cookie HttpOnly.</p>
 *
 * @param accessToken      token de acceso para la cabecera Authorization
 * @param tokenType        tipo de token, siempre {@code Bearer}
 * @param expiresInSeconds segundos de validez del token de acceso
 * @param user             datos públicos del usuario autenticado
 */
@Schema(description = "Sesión iniciada")
public record AuthResponse(

        @Schema(description = "Token de acceso JWT")
        String accessToken,

        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,

        @Schema(description = "Segundos de validez", example = "900")
        long expiresInSeconds,

        @Schema(description = "Usuario autenticado")
        UserResponse user) {
}
