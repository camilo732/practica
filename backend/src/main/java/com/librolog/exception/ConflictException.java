package com.librolog.exception;

/**
 * Conflicto con un recurso existente, por ejemplo un correo ya registrado.
 */
public class ConflictException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje indicado.
     *
     * @param message descripción del conflicto
     */
    public ConflictException(String message) {
        super(message);
    }
}
