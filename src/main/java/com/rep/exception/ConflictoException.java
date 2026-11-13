package com.rep.exception;

import java.util.Map;

/** 409 — duplicados o dependencias que impiden la operación (p. ej. curso con estudiantes). */
public class ConflictoException extends RepException {
    public ConflictoException(ErrorCode code) {
        super(code);
    }

    public ConflictoException(ErrorCode code, String message) {
        super(code, message);
    }

    public ConflictoException(ErrorCode code, Map<String, Object> details) {
        super(code, code.getDefaultMessage(), details);
    }
}
