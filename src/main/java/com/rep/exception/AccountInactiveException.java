package com.rep.exception;

public class AccountInactiveException extends AuthenticationException {
    public AccountInactiveException(String message) {
        super(message);
    }
}