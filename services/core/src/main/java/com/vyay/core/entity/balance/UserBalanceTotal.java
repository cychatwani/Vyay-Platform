package com.vyay.core.entity.balance;

import com.vyay.core.entity.reference.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-user, per-currency rollup of {@link Balance} across all groups. See the
 * V5 migration for the sign convention:
 *   total_owed_minor  = money owed TO the user (positive balances), stored positive.
 *   total_owing_minor = money the user OWES (negative balances), stored as a
 *                       positive magnitude.
 *   net = total_owed_minor - total_owing_minor.
 *
 * Writes never go through JPA — the row is maintained by the native recompute in
 * {@code UserBalanceTotalRepository}, in the same transaction that writes
 * balances. This entity is a read-only projection for the /me/balances endpoint,
 * so it deliberately exposes no setters and does NOT extend the auditable base
 * (there is no created_at; the composite PK also rules out BaseEntity's single id).
 *
 * currency_code is mapped twice: once as the raw UUID PK component (userId /
 * currencyCode) and once as a read-only @ManyToOne so the read path can project
 * the ISO code and symbol without a second query.
 */
@Entity
@Table(name = "user_balance_totals")
@IdClass(UserBalanceTotalId.class)
@Getter
@NoArgsConstructor
public class UserBalanceTotal {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "currency_code", nullable = false)
    private UUID currencyCode;

    @Column(name = "total_owed_minor", nullable = false)
    private long totalOwedMinor;

    @Column(name = "total_owing_minor", nullable = false)
    private long totalOwingMinor;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    /** Read-only view of the currency row; the writable key is {@link #currencyCode}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", insertable = false, updatable = false)
    private Currency currency;

    /** Money the user owes minus money owed to them, in minor units. */
    public long netMinor() {
        return totalOwedMinor - totalOwingMinor;
    }
}
