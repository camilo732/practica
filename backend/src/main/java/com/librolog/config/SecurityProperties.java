package com.librolog.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de seguridad configurables de LibroLog.
 *
 * @param requireHttps            exige HTTPS en todas las peticiones
 * @param hstsEnabled             envía la cabecera HSTS (desactivada en el
 *                                perfil {@code dev} para no fijar
 *                                {@code localhost} a HTTPS en el navegador)
 * @param authRateLimitPerMinute  peticiones por minuto permitidas en
 *                                {@code /api/auth/**} por dirección IP
 * @param maxFailedAttempts       intentos fallidos antes de bloquear la cuenta
 * @param lockMinutes             minutos que dura el bloqueo temporal
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        boolean requireHttps,
        boolean hstsEnabled,
        @Min(1) int authRateLimitPerMinute,
        @Min(1) int maxFailedAttempts,
        @Min(1) int lockMinutes) {
}
