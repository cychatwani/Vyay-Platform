package com.vyay.core.services.generateOtp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class OtpChallenge {

    private final String verificationId;
    private final Instant expiresAt;
    private final Instant resendAvailableAt;
}
