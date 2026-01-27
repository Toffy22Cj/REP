package com.rep.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex) {
                log.warn("Recurso no encontrado: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                                "timestamp", Instant.now().toString(),
                                                "status", 404,
                                                "error", "Not Found",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
                log.warn("Acceso denegado: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of(
                                                "timestamp", Instant.now().toString(),
                                                "status", 403,
                                                "error", "Forbidden",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Object> handleBadRequest(IllegalArgumentException ex) {
                log.warn("Solicitud inválida: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "timestamp", Instant.now().toString(),
                                                "status", 400,
                                                "error", "Bad Request",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<Object> handleMaxUpload(MaxUploadSizeExceededException ex) {
                log.warn("Archivo excede tamaño máximo permitido: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                                .body(Map.of(
                                                "timestamp", Instant.now().toString(),
                                                "status", 413,
                                                "error", "Payload Too Large",
                                                "message", "El archivo excede el tamaño máximo permitido"));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getFieldErrors()
                                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "timestamp", Instant.now().toString(),
                                                "status", 400,
                                                "error", "Validation Error",
                                                "details", errors));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Object> handleGeneric(Exception ex) {
                String trackingId = UUID.randomUUID().toString();
                log.error("Internal Error [trackingId={}]: ", trackingId, ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of(
                                                "timestamp", Instant.now().toString(),
                                                "status", 500,
                                                "error", "Internal Server Error",
                                                "message",
                                                "Se produjo un error interno. Por favor, contacte soporte con el ID de rastreo.",
                                                "trackingId", trackingId));
        }
}
