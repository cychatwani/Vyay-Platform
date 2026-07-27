package com.vyay.core.exception.auth;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("Invalid email or password", "ERR_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
    }
}