package com.rep.exception;

/** 403 — el usuario está autenticado pero no puede acceder a este recurso. */
public class AccessDeniedException extends RepException {
    public AccessDeniedException(String message) {
        super(ErrorCode.ACCESO_DENEGADO, message);
    }

    public AccessDeniedException(ErrorCode code) {
        super(code);
    }
}
