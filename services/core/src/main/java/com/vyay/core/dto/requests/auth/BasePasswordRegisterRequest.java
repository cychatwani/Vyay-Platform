package com.vyay.core.dto.requests.auth;

import com.vyay.core.enums.PasswordRegistrationVerificationChannel;

public interface BasePasswordRegisterRequest {
    String getEmail();
    String getPassword();
    PasswordRegistrationVerificationChannel getVerificationType();   // @NotNull on the impl
}