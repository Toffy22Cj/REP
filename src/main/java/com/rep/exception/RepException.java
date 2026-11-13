package com.rep.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Excepción base de la aplicación. Toda excepción de negocio lleva un {@link ErrorCode}
 * que determina el estado HTTP y el código devuelto al cliente en {@link ApiError}.
 */
public class RepException extends RuntimeException {
    private final ErrorCode code;
    private final Map<String, Object> details;

    public RepException(ErrorCode code) {
        this(code, code.getDefaultMessage(), null, null);
    }

    public RepException(ErrorCode code, String message) {
        this(code, message, null, null);
    }

    public RepException(ErrorCode code, String message, Map<String, Object> details) {
        this(code, message, details, null);
    }

    public RepException(ErrorCode code, String message, Map<String, Object> details, Throwable cause) {
        super(message != null ? message : code.getDefaultMessage(), cause);
        this.code = code;
        this.details = details == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
