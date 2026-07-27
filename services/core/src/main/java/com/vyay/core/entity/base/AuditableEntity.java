package com.vyay.core.entity.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Adds creation/modification timestamps on top of the UUIDv7 id.
 *
 * Both are managed entirely by lifecycle callbacks and have no public setter —
 * like the id, immutable from the outside. createdAt is write-once
 * (updatable = false); updatedAt is bumped on every flush that dirties the row.
 *
 * Extended by aggregates needing standard auditing: Group, Expense, Settlement,
 * Balance. GroupMembership (joinedAt/leftAt) and GroupInviteLink (createdAt-only)
 * don't fit the standard pair, so they sit on BaseEntity directly and declare
 * their own timestamps.
 *
 * This entity's @PrePersist coexists with BaseEntity.assignId() and any
 * subclass @PrePersist (e.g. Expense's expenseDate default); JPA invokes them
 * superclass-first.
 */
@MappedSuperclass
@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class AuditableEntity extends BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {   // respect an explicitly-built value (seed/import)
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}