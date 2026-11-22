package com.librolog.exception;

/**
 * Credenciales incorrectas o cuenta no utilizable.
 *
 * <p>El mensaje es siempre el mismo para no revelar si el usuario existe, si
 * la contraseña es incorrecta o si la cuenta está bloqueada.</p>
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje genérico.
     */
    public InvalidCredentialsException() {
        super("Credenciales inválidas.");
    }
}
