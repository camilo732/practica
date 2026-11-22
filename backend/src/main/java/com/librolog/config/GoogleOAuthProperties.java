package com.librolog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales de Google OAuth2.
 *
 * <p>La integración se activa solo cuando el identificador y el secreto están
 * presentes. Si faltan, LibroLog arranca normalmente y el acceso local sigue
 * disponible.</p>
 *
 * @param clientId     identificador de cliente de Google Cloud
 * @param clientSecret secreto de cliente de Google Cloud
 */
@ConfigurationProperties(prefix = "app.oauth2.google")
public record GoogleOAuthProperties(String clientId, String clientSecret) {

    /**
     * Indica si las credenciales están configuradas.
     *
     * @return verdadero si hay identificador y secreto
     */
    public boolean isConfigured() {
        return hasText(clientId) && hasText(clientSecret);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
