package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class OtpVerificationNotFoundException extends BusinessException {

    public OtpVerificationNotFoundException() {
        super("Verification not found or expired", "ERR_OTP_VERIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
