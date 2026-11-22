package com.librolog.security;

import java.util.Set;

/**
 * Contenido validado de un token de acceso.
 *
 * @param userId   identificador del usuario
 * @param username nombre de usuario
 * @param roles    roles incluidos en el token
 */
public record JwtPayload(Long userId, String username, Set<String> roles) {
}
