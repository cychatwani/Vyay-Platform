package com.vyay.core.dto.response.Auth;

import com.vyay.core.enums.PasswordRegistrationVerificationChannel;
import com.vyay.core.enums.RegistrationNextStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterResponseDTO {

    private String email;

    private boolean verificationRequired;

    private PasswordRegistrationVerificationChannel verificationType;

    private RegistrationNextStep nextStep;

    private String verificationId;

    private Instant expiresAt;

    private Instant resendAvailableAt;

    private String message;
}