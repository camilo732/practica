package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Proveedores de autenticación disponibles.
 *
 * @param local  verdadero si el acceso con correo y contraseña está disponible
 * @param google verdadero si Google OAuth2 está configurado
 */
@Schema(description = "Proveedores de autenticación disponibles")
public record AuthProvidersResponse(

        @Schema(description = "Acceso local disponible", example = "true")
        boolean local,

        @Schema(description = "Acceso con Google disponible", example = "true")
        boolean google) {
}
