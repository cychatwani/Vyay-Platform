package com.vyay.core.entity.expense;

import com.vyay.core.entity.User;
import com.vyay.core.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(
        name = "expense_shares",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_expense_share_expense_user",
                columnNames = {"expense_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ExpenseShare extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Resolved amount this user owes, in MINOR units. ALWAYS persisted, whatever the
    // split type. Sum of all shares == expense.totalAmountMinor.
    @Column(name = "owed_amount_minor", nullable = false)
    private Long owedAmountMinor;

    // Raw split input - kept so the split can be re-rendered and recomputed if the
    // total is edited later. Which one is set depends on expense.splitType:
    //   PERCENTAGE -> percentage (e.g. 33.3333), a ratio so it stays BigDecimal
    //   SHARES     -> shareWeight (e.g. 2)
    //   EQUAL / EXACT -> both null (owedAmountMinor is enough)
    @Column(name = "percentage", precision = 7, scale = 4)
    private BigDecimal percentage;  // nullable

    @Column(name = "share_weight")
    private Integer shareWeight;  // nullable
}