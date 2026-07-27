package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class OtpMaxAttemptsExceededException extends BusinessException {

    public OtpMaxAttemptsExceededException() {
        super("Maximum verification attempts exceeded. Please request a new code.",
                "ERR_OTP_MAX_ATTEMPTS_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
    }
}
