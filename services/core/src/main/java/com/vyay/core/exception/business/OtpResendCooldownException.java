package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class OtpResendCooldownException extends BusinessException {

    private final long retryAfterSeconds;

    public OtpResendCooldownException(long retryAfterSeconds) {
        super("Please wait before requesting a new code",
                "ERR_OTP_RESEND_COOLDOWN", HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
