package com.vyay.core.exception.auth;

import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends AuthException {

    public EmailNotVerifiedException() {
        super("Email address has not been verified", "ERR_EMAIL_NOT_VERIFIED", HttpStatus.FORBIDDEN);
    }
}