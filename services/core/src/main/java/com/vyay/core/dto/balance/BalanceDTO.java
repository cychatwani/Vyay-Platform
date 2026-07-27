package com.vyay.core.dto.balance;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BalanceDTO {
    private String currencyCode;
    private BigDecimal amount;
}