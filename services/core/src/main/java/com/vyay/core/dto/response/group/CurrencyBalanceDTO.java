package com.vyay.core.dto.response.group;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CurrencyBalanceDTO {
    String currencyCode;
    Long netAmountMinor;
    BigDecimal netAmount;
}