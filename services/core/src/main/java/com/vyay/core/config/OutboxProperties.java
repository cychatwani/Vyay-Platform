package com.vyay.core.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Outbox relay tuning. claimTimeout must exceed publishTimeout, or the reaper
 * frees rows a live relay is still publishing and they go out twice.
 */
@ConfigurationProperties(prefix = "app.outbox")
@Validated
public record OutboxProperties(

        boolean relayEnabled,

        @NotNull
        Duration pollInterval,

        @NotNull
        Duration reapInterval,          // how often expired claims are swept

        @Min(1)
        int batchSize,

        @Min(1)
        int maxAttempts,

        @NotNull
        Duration claimTimeout,          // after this, an IN_FLIGHT row is presumed abandoned

        @NotNull
        Duration publishTimeout
) {
}
