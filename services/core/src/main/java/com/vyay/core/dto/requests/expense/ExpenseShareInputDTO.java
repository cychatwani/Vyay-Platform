package com.vyay.core.dto.requests.expense;

import jakarta.validation.constraints.NotNull;
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
public class ExpenseShareInputDTO {

    @NotNull(message = "Participant userId is required")
    private UUID userId;

    private BigDecimal exactAmount;
    private BigDecimal percentage;
    private Integer shareWeight;
}
