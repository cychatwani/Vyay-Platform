package com.vyay.core.dto.requests.verification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailOtpRequestDTO {

    @NotBlank(message = "Verification ID is required")
    private String verificationId;

    @NotBlank(message = "OTP is required")
    private String otp;
}
