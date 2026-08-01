package com.vyay.core.entity.settlement;

import com.vyay.core.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * The link between a {@link SettlementPlanLine} and a real {@link Settlement},
 * recording via {@code appliedAmountMinor} how much of that settlement went to
 * this line. Fulfilment lives ENTIRELY in this join: a line's fulfilled / pending
 * amounts are summed from {@code appliedAmountMinor} of the linked settlements,
 * filtered by settlement status — never stored on the line. So nothing can drift
 * from the settlements table, and a rejected settlement simply drops out of the
 * sums with no unwind.
 *
 * Insert-only and immutable (no setter / updated_at / version). Reconciliation
 * writes these in SettlementService; the client never does.
 *
 * Unique per (planLine, settlement): a settlement fulfils a given line at most
 * once, but MAY fulfil several different lines — one row each, e.g. a 120-unit
 * settlement splitting 100 onto one line and 20 onto another. appliedAmountMinor
 * is what keeps those from both reading as 120.
 */
@Entity
@Table(
        name = "settlement_plan_line_fulfillment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_settlement_plan_line_fulfillment_line_settlement",
                columnNames = {"plan_line_id", "settlement_id"}
        )
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SettlementPlanLineFulfillment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_line_id", nullable = false)
    private SettlementPlanLine planLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    /** How much of {@link #settlement} applied to {@link #planLine} (always &gt; 0). */
    @Column(name = "applied_amount_minor", nullable = false)
    private Long appliedAmountMinor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
