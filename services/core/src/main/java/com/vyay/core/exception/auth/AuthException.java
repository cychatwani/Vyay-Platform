package com.vyay.core.exception.auth;

import org.springframework.http.HttpStatus;

public abstract class AuthException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected AuthException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}