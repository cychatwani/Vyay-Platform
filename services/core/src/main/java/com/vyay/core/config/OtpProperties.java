package com.vyay.core.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.otp")
@Validated
public record OtpProperties(

        @Min(4) @Max(8)
        int length,

        @NotNull
        Duration ttl,

        @Min(1)
        int maxAttempts,

        @NotNull
        Duration resendCooldown
) {
}