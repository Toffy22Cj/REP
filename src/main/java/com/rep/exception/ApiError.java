package com.rep.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo JSON uniforme para todas las respuestas de error de la API.
 *
 * <pre>
 * {
 *   "timestamp": "2026-11-12T22:10:00Z",
 *   "status": 409,
 *   "error": "Conflict",
 *   "code": "USUARIO_IDENTIFICACION_DUPLICADA",
 *   "message": "Ya existe un usuario con esa identificación",
 *   "path": "/api/admin/usuarios",
 *   "details": { "identificacion": "1098765432" },
 *   "traceId": "..."            // solo en errores internos
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        String timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, Object> details,
        String traceId) {

    public static ApiError of(ErrorCode code, String message, String path, Map<String, Object> details, String traceId) {
        return new ApiError(
                Instant.now().toString(),
                code.getStatus().value(),
                code.getStatus().getReasonPhrase(),
                code.name(),
                message != null ? message : code.getDefaultMessage(),
                path,
                details,
                traceId);
    }

    public static ApiError of(ErrorCode code, String path) {
        return of(code, null, path, null, null);
    }
}
