package com.librolog.entity.enums;

/**
 * Proveedor de autenticación de un usuario.
 */
public enum AuthProvider {

    /** Usuario registrado con correo y contraseña. */
    LOCAL,

    /** Usuario autenticado mediante Google OAuth2. */
    GOOGLE
}
