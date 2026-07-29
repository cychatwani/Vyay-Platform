package com.vyay.core.entity.settlement;

import com.vyay.core.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * The link between a {@link SettlementPlanLine} and a real {@link Settlement}
 * that (partially) fulfils it. Fulfilment lives ENTIRELY in this join: a line's
 * fulfilled / pending / remaining amounts are summed from the settlements linked
 * here, never stored on the line. So nothing can drift from the settlements
 * table, and a rejected settlement simply drops out of the sums with no unwind.
 *
 * Insert-only and immutable (no setter / updated_at / version). Reconciliation
 * writes these in SettlementService; the client never does.
 *
 * settlement_id is unique (a settlement fulfils at most one line); plan_line_id
 * is intentionally NOT unique (a line may be fulfilled by several settlements).
 */
@Entity
@Table(
        name = "settlement_plan_line_fulfillment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_settlement_plan_line_fulfillment_settlement",
                columnNames = {"settlement_id"}
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
