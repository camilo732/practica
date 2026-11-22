package com.librolog.exception;

/**
 * Refresh token ausente, caducado, revocado o reutilizado.
 */
public class InvalidTokenException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje genérico de sesión no válida.
     */
    public InvalidTokenException() {
        super("Sesión no válida. Inicia sesión de nuevo.");
    }
}
