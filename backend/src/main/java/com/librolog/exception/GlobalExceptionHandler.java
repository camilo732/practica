package com.librolog.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Manejador global de excepciones que devuelve {@link ProblemDetail}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores de validación de Bean Validation.
     *
     * @param ex excepción de validación
     * @return problema 400 con el detalle por campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos inválidos.");
        problem.setTitle("Error de validación");

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Maneja recursos no encontrados.
     *
     * @param ex excepción de recurso ausente
     * @return problema 404
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoSuchElementException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage() == null ? "Recurso no encontrado." : ex.getMessage());
        problem.setTitle("Recurso no encontrado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Maneja parámetros con un tipo que no se puede convertir.
     *
     * @param ex excepción de conversión
     * @return problema 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' no tiene un valor válido.");
        problem.setTitle("Solicitud inválida");
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Maneja argumentos inválidos.
     *
     * @param ex excepción de argumento inválido
     * @return problema 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() == null ? "Solicitud inválida." : ex.getMessage());
        problem.setTitle("Solicitud inválida");
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Maneja credenciales incorrectas con un mensaje genérico.
     *
     * @param ex excepción de credenciales inválidas
     * @return problema 401
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas.");
        problem.setTitle("No autorizado");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Maneja refresh tokens ausentes, caducados o reutilizados.
     *
     * @param ex excepción de token inválido
     * @return problema 401
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidToken(InvalidTokenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Sesión no válida. Inicia sesión de nuevo.");
        problem.setTitle("Sesión no válida");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Bearer")
                .body(problem);
    }

    /**
     * Maneja conflictos de negocio, como un correo ya registrado.
     *
     * @param ex excepción de conflicto
     * @return problema 409
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage() == null ? "El recurso ya existe." : ex.getMessage());
        problem.setTitle("Conflicto");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Maneja violaciones de restricciones de la base de datos sin filtrar SQL.
     *
     * @param ex excepción de integridad
     * @return problema 409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Conflicto de integridad de datos", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "No se pudo completar la operación por un conflicto con datos existentes.");
        problem.setTitle("Conflicto");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Maneja cuerpos de petición ilegibles o con formato incorrecto.
     *
     * @param ex excepción de lectura del cuerpo
     * @return problema 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido o tiene un formato incorrecto.");
        problem.setTitle("Solicitud inválida");
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Maneja la indisponibilidad de un servicio externo sin datos en caché.
     *
     * @param ex excepción de servicio no disponible
     * @return problema 503
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleServiceUnavailable(ServiceUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage() == null
                        ? "El servicio no está disponible en este momento."
                        : ex.getMessage());
        problem.setTitle("Servicio no disponible");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "60")
                .body(problem);
    }

    /**
     * Maneja errores no controlados sin exponer la traza al cliente.
     *
     * @param ex excepción inesperada
     * @return problema 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Inténtalo de nuevo.");
        problem.setTitle("Error interno");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
