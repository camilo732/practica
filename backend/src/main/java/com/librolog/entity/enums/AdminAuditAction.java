package com.librolog.entity.enums;

/**
 * Acciones administrativas que quedan registradas en la auditoría.
 */
public enum AdminAuditAction {

    /** Se habilitó una cuenta. */
    USER_ENABLED,

    /** Se deshabilitó una cuenta. */
    USER_DISABLED,

    /** Se concedió el rol de administrador. */
    ROLE_GRANTED,

    /** Se retiró el rol de administrador. */
    ROLE_REVOKED,

    /** Se desbloqueó una cuenta tras varios intentos fallidos. */
    USER_UNLOCKED,

    /** Se cerraron todas las sesiones de una cuenta. */
    SESSIONS_REVOKED
}
