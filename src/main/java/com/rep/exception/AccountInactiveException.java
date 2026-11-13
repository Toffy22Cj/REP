package com.rep.exception;

/** 403 — la cuenta existe y las credenciales son correctas, pero está desactivada. */
public class AccountInactiveException extends AuthenticationException {
    public AccountInactiveException(String message) {
        super(ErrorCode.CUENTA_INACTIVA, message);
    }
}
