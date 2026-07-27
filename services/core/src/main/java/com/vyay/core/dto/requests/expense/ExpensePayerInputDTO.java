package com.vyay.core.dto.requests.expense;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpensePayerInputDTO {

    @NotNull(message = "Payer userId is required")
    private UUID userId;

    @NotNull(message = "amountPaid is required")
    @Positive(message = "amountPaid must be positive")
    private BigDecimal amountPaid;
}
