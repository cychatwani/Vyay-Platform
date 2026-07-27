package com.vyay.core.dto.response.expense;

import com.vyay.core.common.utils.MoneyUtils;
import com.vyay.core.entity.expense.ExpensePayer;
import com.vyay.core.entity.reference.Currency;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ExpensePayerResponseDTO {

    private UUID userId;
    private Long amountPaidMinor;
    private BigDecimal amountPaid;

    public static ExpensePayerResponseDTO from(ExpensePayer p, Currency currency) {
        return ExpensePayerResponseDTO.builder()
                .userId(p.getUser().getId())
                .amountPaidMinor(p.getAmountPaidMinor())
                .amountPaid(MoneyUtils.toMajor(p.getAmountPaidMinor(), currency))
                .build();
    }
}
