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
 * Generation writes two kinds of line, and neither is appended:
 *   - carried: one per in-flight PROPOSED settlement, at that settlement's exact
 *     from / to / amount, linked to it so the line reads as fully pending;
 *   - netted: the output of the calculator over the adjusted basis.
 *
 * Deliberately NOT unique per (plan, fromUser, toUser): a plan may hold several
 * lines for the same ordered pair — two settlements in flight between the same
 * two people, a carried line alongside a netted one, or a line appended later.
 * Reconciliation matches on the pair and fills those lines oldest-first by id
 * (UUIDv7 is time-ordered).
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

    /**
     * Written by whoever creates the line, never inferred: false for a generation
     * batch, true for a line appended to a live plan after a balance change. The
     * visible seam between "what I originally agreed to" and "what turned up
     * later" is the whole point of the append model, so it is stored as a fact
     * rather than derived from comparing this row's clock to the plan's.
     */
    @Column(name = "appended", nullable = false)
    @Builder.Default
    private boolean appended = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
