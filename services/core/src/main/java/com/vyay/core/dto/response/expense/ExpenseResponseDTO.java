package com.vyay.core.dto.response.expense;

import com.vyay.core.common.utils.MoneyUtils;
import com.vyay.core.entity.expense.Expense;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.SplitType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ExpenseResponseDTO {

    private UUID expenseId;
    private String description;
    private Long totalAmountMinor;
    private BigDecimal totalAmount;
    private String currencyCode;
    private String currencySymbol;
    private UUID groupId;
    private UUID createdBy;
    private SplitType splitType;
    private Instant expenseDate;
    private String notes;
    private Instant createdAt;
    private List<ExpensePayerResponseDTO> payers;
    private List<ExpenseShareResponseDTO> shares;

    public static ExpenseResponseDTO from(Expense e) {
        Currency c = e.getCurrency();
        return ExpenseResponseDTO.builder()
                .expenseId(e.getId())
                .description(e.getDescription())
                .totalAmountMinor(e.getTotalAmountMinor())
                .totalAmount(MoneyUtils.toMajor(e.getTotalAmountMinor(), c))
                .currencyCode(c.getCode())
                .currencySymbol(c.getSymbol())
                .groupId(e.getGroup() != null ? e.getGroup().getId() : null)
                .createdBy(e.getCreatedBy().getId())
                .splitType(e.getSplitType())
                .expenseDate(e.getExpenseDate())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .payers(e.getPayers().stream().map(p -> ExpensePayerResponseDTO.from(p, c)).toList())
                .shares(e.getShares().stream().map(s -> ExpenseShareResponseDTO.from(s, c)).toList())
                .build();
    }
}
