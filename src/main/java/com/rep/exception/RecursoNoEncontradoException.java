package com.rep.exception;

/** 404 — el recurso (usuario, curso, actividad...) no existe. */
public class RecursoNoEncontradoException extends RepException {
    public RecursoNoEncontradoException(String mensaje) {
        super(ErrorCode.RECURSO_NO_ENCONTRADO, mensaje);
    }

    public RecursoNoEncontradoException(ErrorCode code) {
        super(code);
    }

    public RecursoNoEncontradoException(ErrorCode code, Object id) {
        super(code, code.getDefaultMessage() + " (id " + id + ")", java.util.Map.of("id", String.valueOf(id)));
    }
}
