package com.vyay.core.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Soft-delete DOMAIN STATE on top of the audited UUIDv7 aggregate: a deletedAt
 * timestamp (null = live) and a derived isDeleted(). Nothing else.
 *
 * Deliberately carries NO query behavior. "deletedAt" is a domain concern;
 * "hide deleted rows from reads" is a separate query concern, declared per
 * concrete entity next to the @SQLDelete it pairs with:
 *
 *   @Entity
 *   @SQLDelete(sql = "update expenses set deleted_at = now() where id = ?")
 *   @SQLRestriction("deleted_at is null")
 *   public class Expense extends SoftDeletableEntity { ... }
 *
 * Keeping @SQLRestriction per-entity (rather than on this base) lets an entity
 * support soft deletion WITHOUT being auto-filtered:
 *   - Group / Expense / Settlement -> deletedAt + @SQLDelete + @SQLRestriction
 *   - User                         -> deletedAt only, no filter; deleted users
 *                                     stay queryable so historical FK references
 *                                     (created_by, payer, ...) still resolve.
 *                                     Exclusion from active use is enforced in
 *                                     the auth/active-user queries.
 *
 * GroupMembership is still NOT here — it sits on BaseEntity with joinedAt/leftAt
 * plus its own deletedAt, since its timestamps don't match the audited pair.
 *
 * No setter for deletedAt: deletion happens at the repository layer (per-entity
 * @SQLDelete, or an atomic @Modifying update), keeping the field immutable from
 * application code like id/createdAt.
 */
@MappedSuperclass
@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class SoftDeletableEntity extends AuditableEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;  // null = live; non-null = soft-deleted

    /** Derived flag — replaces the old stored isDeleted boolean. */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}