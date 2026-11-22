package com.librolog.exception;

/**
 * Error de negocio al completar el acceso con Google.
 *
 * <p>El mensaje se registra en el servidor, pero nunca se muestra al navegador:
 * el usuario vuelve al formulario de acceso con un aviso genérico.</p>
 */
public class OAuthLoginException extends RuntimeException {

    /**
     * Crea la excepción con un motivo interno.
     *
     * @param message motivo
     */
    public OAuthLoginException(String message) {
        super(message);
    }
}
