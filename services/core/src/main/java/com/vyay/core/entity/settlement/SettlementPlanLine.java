package com.vyay.core.entity.settlement;

import com.vyay.core.entity.User;
import com.vyay.core.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * One directed transfer a plan proposes: {@code fromUser} (debtor) pays
 * {@code toUser} (creditor) {@code amountMinor}.
 *
 * Immutable — a line never changes payer, payee, or amount, only its execution
 * state — so there is no setter, no updated_at, and no version. There is also NO
 * status column: line progress (PENDING / PARTIALLY_FULFILLED / COMPLETED) is
 * DERIVED from the settlements linked via {@link SettlementPlanLineFulfillment},
 * never stored.
 *
 * Deliberately NOT unique per (plan, fromUser, toUser): the append model lets a
 * plan carry several lines for the same ordered pair (an original line plus a
 * later appended one). Reconciliation matches on the pair and fills those lines
 * oldest-first by id (UUIDv7 is time-ordered). An appended line is simply one
 * whose createdAt is later than its plan's.
 */
@Entity
@Table(name = "settlement_plan_line")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SettlementPlanLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SettlementPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
