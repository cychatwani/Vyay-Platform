package com.vyay.core.entity.settlement;

import com.vyay.core.entity.User;
import com.vyay.core.entity.base.BaseEntity;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.SettlementPlanStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

/**
 * A persisted settle-up plan: the netting of one group's balances (for a single
 * currency) into a set of directed transfers ({@link SettlementPlanLine}) that
 * members execute over time.
 *
 * Sits on {@link BaseEntity} (id only) and declares its own created/updated
 * timestamps — deliberately NOT {@code AuditableEntity}, because it carries no
 * {@code @Version}. The only mutable field is {@code status}, and every
 * transition is a guarded compare-and-set UPDATE ({@code WHERE status = expected})
 * backed by the "one live plan" partial index — a zero row-count is the
 * "someone beat me to it" signal. That closes the same races optimistic locking
 * would, without a retry loop on the staleness hot path (which fires on every
 * expense). updated_at is set by those native updates, so @PreUpdate here is
 * only a safety net for any JPA-path mutation.
 */
@Entity
@Table(name = "settlement_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SettlementPlan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "settlement_plan_status", nullable = false)
    @Builder.Default
    private SettlementPlanStatus status = SettlementPlanStatus.ACTIVE;

    /**
     * Set on THIS (old) plan when a regeneration supersedes it — points forward
     * to the replacement. Written by the native supersede UPDATE, so it is a raw
     * id pointer rather than a navigable association. Null until superseded.
     */
    @Column(name = "superseded_by")
    private UUID supersededBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
