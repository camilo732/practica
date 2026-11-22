package com.librolog.security;

import java.util.Set;

/**
 * Usuario autenticado que viaja en el contexto de seguridad.
 *
 * @param id       identificador del usuario
 * @param username nombre de usuario
 * @param roles    nombres de los roles, sin el prefijo {@code ROLE_}
 */
public record AuthenticatedUser(Long id, String username, Set<String> roles) {
}
