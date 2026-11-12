package com.rep.exception;

/** 400 — se superó un límite permitido (p. ej. número de opciones). */
public class LimiteExcedidoException extends RepException {
    public LimiteExcedidoException(String message) {
        super(ErrorCode.LIMITE_EXCEDIDO, message);
    }
}
