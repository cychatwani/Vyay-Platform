package com.vyay.core.repository;

import com.vyay.core.entity.balance.UserBalanceTotal;
import com.vyay.core.entity.balance.UserBalanceTotalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserBalanceTotalRepository
        extends JpaRepository<UserBalanceTotal, UserBalanceTotalId> {

    /**
     * Step 1 of the in-transaction recompute: materialise a zeroed row for every
     * (user, currency) pair the affected users currently have in {@code balances}.
     * ON CONFLICT DO NOTHING makes it idempotent. This exists so that step 2's
     * SELECT ... FOR UPDATE always has a row to lock, closing the first-touch race
     * where two concurrent transactions would otherwise both create the totals row
     * and one would lose the other's contribution.
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_balance_totals (user_id, currency_code, total_owed_minor, total_owing_minor, updated_at, version)
            SELECT b.user_id, b.currency_code, 0, 0, now(), 0
            FROM balances b
            WHERE b.user_id = ANY(:userIds)
            GROUP BY b.user_id, b.currency_code
            ON CONFLICT (user_id, currency_code) DO NOTHING
            """, nativeQuery = true)
    void ensureTotalsRows(@Param("userIds") UUID[] userIds);

    /**
     * Step 2: take a row lock on every totals row for the affected users, in a
     * deterministic order (user_id, currency_code) to avoid deadlocks between
     * transactions locking overlapping user sets. Holding these locks serialises
     * the recompute in step 3: a concurrent transaction touching the same user
     * blocks here until we commit, so its subsequent aggregate read (a fresh
     * READ COMMITTED snapshot) sees our committed balances rather than overwriting
     * them with a stale sum.
     */
    @Query(value = """
            SELECT t.user_id
            FROM user_balance_totals t
            WHERE t.user_id = ANY(:userIds)
            ORDER BY t.user_id, t.currency_code
            FOR UPDATE
            """, nativeQuery = true)
    List<UUID> lockTotalsRows(@Param("userIds") UUID[] userIds);

    /**
     * Step 3: recompute the totals for the affected users directly from balances,
     * in a single statement. Runs while holding the step-2 locks. The INSERT branch
     * is effectively unreachable (step 1 already created the rows) but keeps the
     * statement self-healing.
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_balance_totals (user_id, currency_code, total_owed_minor, total_owing_minor, updated_at, version)
            SELECT b.user_id, b.currency_code,
                   COALESCE(SUM(b.net_amount_minor) FILTER (WHERE b.net_amount_minor > 0), 0),
                   COALESCE(SUM(-b.net_amount_minor) FILTER (WHERE b.net_amount_minor < 0), 0),
                   now(), 0
            FROM balances b
            WHERE b.user_id = ANY(:userIds)
            GROUP BY b.user_id, b.currency_code
            ON CONFLICT (user_id, currency_code)
            DO UPDATE SET total_owed_minor  = EXCLUDED.total_owed_minor,
                          total_owing_minor = EXCLUDED.total_owing_minor,
                          updated_at        = now(),
                          version           = user_balance_totals.version + 1
            """, nativeQuery = true)
    void recomputeTotals(@Param("userIds") UUID[] userIds);

    /**
     * Read path for /me/balances. currency is eagerly fetched so the DTO can
     * project the ISO code and symbol without a per-row lazy load.
     */
    @Query("SELECT t FROM UserBalanceTotal t JOIN FETCH t.currency WHERE t.userId = :userId")
    List<UserBalanceTotal> findByUserIdWithCurrency(@Param("userId") UUID userId);
}
