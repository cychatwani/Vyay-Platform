package com.vyay.core.dto.response.expense;

import com.vyay.core.common.utils.MoneyUtils;
import com.vyay.core.entity.expense.ExpenseShare;
import com.vyay.core.entity.reference.Currency;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ExpenseShareResponseDTO {

    private UUID userId;
    private Long owedAmountMinor;
    private BigDecimal owedAmount;
    private BigDecimal percentage;
    private Integer shareWeight;

    public static ExpenseShareResponseDTO from(ExpenseShare s, Currency currency) {
        return ExpenseShareResponseDTO.builder()
                .userId(s.getUser().getId())
                .owedAmountMinor(s.getOwedAmountMinor())
                .owedAmount(MoneyUtils.toMajor(s.getOwedAmountMinor(), currency))
                .percentage(s.getPercentage())
                .shareWeight(s.getShareWeight())
                .build();
    }
}
