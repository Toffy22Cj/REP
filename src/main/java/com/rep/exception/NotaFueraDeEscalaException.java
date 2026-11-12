package com.rep.exception;

/** 400 — la nota está fuera de la escala institucional 0.0 – 5.0. */
public class NotaFueraDeEscalaException extends ValidacionException {
    public NotaFueraDeEscalaException(String message) {
        super(ErrorCode.NOTA_FUERA_DE_ESCALA, message);
    }
}
