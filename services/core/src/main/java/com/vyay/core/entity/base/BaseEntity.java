package com.vyay.core.entity.base;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Foundation shape for every aggregate: a single UUIDv7 primary key, stored as
 * a native PostgreSQL {@code uuid}, time-ordered for B-tree locality, and used
 * as the one-and-only identifier — persisted, joined on, emitted in events, and
 * exposed in APIs (renamed to a semantic field, e.g. userId/groupId, in DTOs).
 *
 * The field name stays generic ({@code id}) so the persistence layer and
 * Repository.findById(...) remain aggregate-agnostic.
 *
 * Generation is app-side in @PrePersist via UuidCreator.getTimeOrderedEpoch()
 * (RFC-9562 v7). Note: java.util.UUID.randomUUID() is v4/random and must NOT be
 * used — it would forfeit the time-ordering this migration exists for.
 *
 * Deliberately NOT extended by:
 *   - BalanceLedgerEntry  (id is hand-assigned by the service BEFORE HMAC
 *                          signing and is part of the signed payload; an
 *                          inherited callback that fills a null id at persist
 *                          would produce stored id != signed id -> verify fails)
 *
 * SuperBuilder (not @Builder) so subclass builders include this id; the
 * .builder() fluent API is unchanged for callers.
 */
@MappedSuperclass
@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;  // UUIDv7, sole aggregate id — no setter, immutable after construction

    /**
     * Assigns a UUIDv7 on first persist if one wasn't set explicitly. Distinct
     * method name (not "prePersist") so subclasses adding their own @PrePersist
     * — e.g. Expense defaulting expenseDate — coexist; JPA fires both, base first      .
     */
    @PrePersist
    protected void assignId() {
        if (id == null) {
            id = UuidCreator.getTimeOrderedEpoch();
            System.out.println(id);
            System.out.println(id.version());
        }
    }
}