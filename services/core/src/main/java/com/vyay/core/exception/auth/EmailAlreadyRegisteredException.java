package com.vyay.core.exception.auth;

import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends AuthException {

    public EmailAlreadyRegisteredException() {
        super("An account with this email already exists", "ERR_EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
    }
}