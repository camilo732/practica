package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Información pública de la API.
 *
 * @param name          nombre de la aplicación
 * @param version       versión actual
 * @param status        estado del servicio
 * @param documentation ruta de Swagger UI
 */
@Schema(description = "Información pública de LibroLog")
public record AppInfoResponse(

        @Schema(description = "Nombre de la aplicación", example = "LibroLog")
        String name,

        @Schema(description = "Versión de la API", example = "1.0.0")
        String version,

        @Schema(description = "Estado del servicio", example = "UP")
        String status,

        @Schema(description = "Ruta de la documentación Swagger", example = "/swagger-ui.html")
        String documentation
) {
}
