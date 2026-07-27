package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends BusinessException {

    private final int remainingAttempts;

    public InvalidOtpException(int remainingAttempts) {
        super("Invalid verification code. " + remainingAttempts + " attempt(s) remaining.",
                "ERR_INVALID_OTP", HttpStatus.BAD_REQUEST);
        this.remainingAttempts = remainingAttempts;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}
