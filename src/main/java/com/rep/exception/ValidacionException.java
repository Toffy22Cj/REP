package com.rep.exception;

import java.util.Map;

/** 400 — los datos no cumplen una regla de negocio. */
public class ValidacionException extends RepException {
    public ValidacionException(String message) {
        super(ErrorCode.VALIDACION_FALLIDA, message);
    }

    public ValidacionException(ErrorCode code) {
        super(code);
    }

    public ValidacionException(ErrorCode code, String message) {
        super(code, message);
    }

    public ValidacionException(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
