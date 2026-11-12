package com.rep.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Traduce todas las excepciones a un {@link ApiError} con código de error estable.
 *
 * <ul>
 *   <li>{@link RepException} y subclases: código y estado definidos en {@link ErrorCode}.</li>
 *   <li>Errores del framework (validación, JSON, parámetros, integridad...): códigos genéricos.</li>
 *   <li>Cualquier otra excepción: {@link ErrorCode#ERROR_INTERNO} con un traceId que se
 *       registra en el log junto con la traza completa; nunca se expone el mensaje interno.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ negocio

    @ExceptionHandler(RepException.class)
    public ResponseEntity<ApiError> handleRep(RepException ex, HttpServletRequest request) {
        ErrorCode code = ex.getCode();
        if (code.getStatus().is5xxServerError()) {
            return internal(ex, code, request);
        }
        log.info("{} {} -> {} ({})", request.getMethod(), request.getRequestURI(), code, ex.getMessage());
        return build(code, ex.getMessage(), request, ex.getDetails());
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleJpaNotFound(jakarta.persistence.EntityNotFoundException ex,
            HttpServletRequest request) {
        return build(ErrorCode.RECURSO_NO_ENCONTRADO, ex.getMessage(), request, null);
    }

    /** Validaciones de servicios que aún usan las excepciones estándar de Java. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.info("{} {} -> VALIDACION_FALLIDA ({})", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(ErrorCode.VALIDACION_FALLIDA, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.info("{} {} -> OPERACION_NO_PERMITIDA ({})", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(ErrorCode.OPERACION_NO_PERMITIDA, ex.getMessage(), request, null);
    }

    // ------------------------------------------------------------------ framework

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(ErrorCode.CAMPOS_INVALIDOS, resumen(campos), request, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> campos.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return build(ErrorCode.CAMPOS_INVALIDOS, resumen(campos), request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(ErrorCode.JSON_MALFORMADO, null, request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return build(ErrorCode.PARAMETRO_FALTANTE, "Falta el parámetro obligatorio '" + ex.getParameterName() + "'",
                request, Map.of("parametro", ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(ErrorCode.PARAMETRO_INVALIDO, "El parámetro '" + ex.getName() + "' tiene un valor no válido",
                request, Map.of("parametro", ex.getName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(ErrorCode.METODO_NO_PERMITIDO, null, request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(ErrorCode.RUTA_NO_ENCONTRADA, null, request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violación de integridad en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.CONFLICTO_DATOS, null, request, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(ErrorCode.ARCHIVO_DEMASIADO_GRANDE, null, request, null);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleSpringAccessDenied(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.ACCESO_DENEGADO, null, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        return internal(ex, ErrorCode.ERROR_INTERNO, request);
    }

    // ------------------------------------------------------------------ helpers

    private static ResponseEntity<ApiError> internal(Exception ex, ErrorCode code, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("Error interno [traceId={}] en {} {}", traceId, request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(code.getStatus())
                .body(ApiError.of(code, code.getDefaultMessage(), request.getRequestURI(), null, traceId));
    }

    private static ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest request,
            Map<String, Object> details) {
        return ResponseEntity.status(code.getStatus())
                .body(ApiError.of(code, message, request.getRequestURI(), details, null));
    }

    private static String resumen(Map<String, Object> campos) {
        if (campos.isEmpty()) {
            return ErrorCode.CAMPOS_INVALIDOS.getDefaultMessage();
        }
        StringBuilder sb = new StringBuilder();
        campos.forEach((campo, msg) -> sb.append(sb.isEmpty() ? "" : "; ").append(campo).append(": ").append(msg));
        return sb.toString();
    }
}
