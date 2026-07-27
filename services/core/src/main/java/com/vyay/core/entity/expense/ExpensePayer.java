package com.vyay.core.entity.expense;

import com.vyay.core.entity.User;
import com.vyay.core.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "expense_payers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_expense_payer_expense_user",
                columnNames = {"expense_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ExpensePayer extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Minor units of expense.currency. Sum of all payers == expense.totalAmountMinor.
    @Column(name = "amount_paid_minor", nullable = false)
    private Long amountPaidMinor;  // money, stays Long; mutable
}