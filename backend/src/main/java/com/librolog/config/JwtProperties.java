package com.librolog.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades del token JWT.
 *
 * @param secret             clave HMAC leída de {@code JWT_SECRET}; debe tener
 *                           al menos 32 caracteres
 * @param accessTokenMinutes minutos de vida del token de acceso
 * @param refreshTokenDays   días de vida del refresh token
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Min(1) int accessTokenMinutes,
        @Min(1) int refreshTokenDays) {
}
