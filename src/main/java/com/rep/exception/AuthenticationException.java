package com.rep.exception;

/** 401 — credenciales incorrectas. */
public class AuthenticationException extends RepException {
    public AuthenticationException(String message) {
        super(ErrorCode.CREDENCIALES_INVALIDAS, message);
    }

    protected AuthenticationException(ErrorCode code, String message) {
        super(code, message);
    }
}
