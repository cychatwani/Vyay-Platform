package com.vyay.core.entity.balance;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for {@link UserBalanceTotal}: (user_id, currency_code).
 *
 * Both fields hold the UUID PKs of the referenced rows (currency_code is the
 * Currency PK, not the ISO code). Field names must match the @Id fields on the
 * entity for JPA to bind the IdClass.
 */
public class UserBalanceTotalId implements Serializable {

    private UUID userId;
    private UUID currencyCode;

    public UserBalanceTotalId() {
    }

    public UserBalanceTotalId(UUID userId, UUID currencyCode) {
        this.userId = userId;
        this.currencyCode = currencyCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserBalanceTotalId that)) return false;
        return Objects.equals(userId, that.userId)
                && Objects.equals(currencyCode, that.currencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, currencyCode);
    }
}
