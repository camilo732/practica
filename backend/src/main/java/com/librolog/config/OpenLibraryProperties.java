package com.librolog.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de la integración con Open Library.
 *
 * @param baseUrl          URL base de la API pública
 * @param userAgent        agente de usuario identificativo que exige Open Library
 * @param connectTimeoutMs tiempo máximo para abrir la conexión
 * @param readTimeoutMs    tiempo máximo de espera de la respuesta
 */
@Validated
@ConfigurationProperties(prefix = "app.openlibrary")
public record OpenLibraryProperties(
        @NotBlank String baseUrl,
        @NotBlank String userAgent,
        @Min(500) int connectTimeoutMs,
        @Min(500) int readTimeoutMs) {
}
