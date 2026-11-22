package com.librolog.exception;

/**
 * El servicio externo no responde y no hay datos locales con los que responder.
 *
 * <p>Se traduce en un {@code 503} para que el cliente sepa que puede reintentar
 * más tarde y no lo confunda con un error de la aplicación.</p>
 */
public class ServiceUnavailableException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje indicado.
     *
     * @param message explicación para la persona usuaria
     */
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
