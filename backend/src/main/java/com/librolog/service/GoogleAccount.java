package com.librolog.service;

/**
 * Datos mínimos que Google devuelve tras autenticar a una persona.
 *
 * @param subject       identificador estable de la cuenta de Google
 * @param email         correo electrónico
 * @param emailVerified verdadero si Google confirma el correo
 * @param name          nombre completo
 * @param locale        idioma declarado por Google
 */
public record GoogleAccount(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String locale) {
}
