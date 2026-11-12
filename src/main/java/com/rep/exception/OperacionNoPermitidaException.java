package com.rep.exception;

/** 409 — la operación no es posible en el estado actual del recurso. */
public class OperacionNoPermitidaException extends RepException {
    public OperacionNoPermitidaException(String message) {
        super(ErrorCode.OPERACION_NO_PERMITIDA, message);
    }

    public OperacionNoPermitidaException(ErrorCode code) {
        super(code);
    }

    public OperacionNoPermitidaException(ErrorCode code, String message) {
        super(code, message);
    }
}
