package com.rep.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;

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

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex) {
                log.warn("Error de autenticación: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                                "timestamp", Instant.now().toString(),
                                                "status", 401,
                                                "error", "Unauthorized",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Object> handleGeneric(Exception ex) {
                log.error("Error interno del servidor", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of(
                                                "timestamp", Instant.now().toString(),
                                                "status", 500,
                                                "error", "Internal Server Error",
                                                "message", "Se produjo un error interno"));
        }
}
