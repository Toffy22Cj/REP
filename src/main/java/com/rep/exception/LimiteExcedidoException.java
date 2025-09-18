package com.rep.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LimiteExcedidoException extends RuntimeException {
    public LimiteExcedidoException(String message) {
        super(message);
    }
}