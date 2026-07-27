package com.vyay.core.dto.requests.settlement;

import com.vyay.core.enums.SettlementMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Shared fields for every settlement creation request. The party ids differ per
 * endpoint (each concrete DTO names only the role the principal is NOT), so they
 * live on the subclasses; everything common lives here.
 * <p>
 * Abstract on purpose: each endpoint's @RequestBody is typed to a concrete
 * subclass, so Jackson always binds a concrete type and never this base.
 * <p>
 * Amount is in MAJOR currency units (e.g. 250.00), consistent with the expense
 * API; the service converts to minor units via the currency's decimal places.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class SettlementRequestBase {

    @NotNull(message = "Currency code is required")
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "Currency code must be a valid ISO 4217 code format (e.g. INR, USD)"
    )
    private String currencyCode;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    // Optional: how the money moved. Null until known.
    private SettlementMethod method;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;
}