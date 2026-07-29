package com.vyay.core.repository;

import com.vyay.core.entity.settlement.SettlementPlan;
import com.vyay.core.enums.SettlementPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SettlementPlanRepository extends JpaRepository<SettlementPlan, UUID> {

    /**
     * The plan for a (group, currency) whose status is in the given set — used
     * for the live lookup (pass {ACTIVE, STALE}) and the reconciliation target
     * (pass {ACTIVE}). At most one row exists for the live set, guaranteed by the
     * partial unique index, so Optional is safe. currency is fetched for the read
     * model.
     */
    @Query("""
            select p from SettlementPlan p
            join fetch p.currency
            where p.group.id = :groupId
              and p.currency.id = :currencyId
              and p.status in :statuses
            """)
    Optional<SettlementPlan> findByGroupAndCurrencyAndStatusIn(
            @Param("groupId") UUID groupId,
            @Param("currencyId") UUID currencyId,
            @Param("statuses") Collection<SettlementPlanStatus> statuses);

    // -----------------------------------------------------------------
    // Guarded compare-and-set transitions.
    //
    // Each returns the affected row count: 0 means the plan was not in the
    // expected state (someone else transitioned it first), which is the whole
    // concurrency guard — no @Version, no retry loop. updated_at is set here
    // because these bypass the JPA lifecycle. Enum literals compare/assign
    // against the settlement_plan_status column directly.
    // -----------------------------------------------------------------

    /**
     * Stale the ACTIVE plan for a (group, currency). Idempotent hot path: called
     * on every expense and every off-plan confirmed settlement. Only ACTIVE ->
     * STALE; a STALE plan stays STALE and terminal plans are untouched.
     */
    @Modifying
    @Query(value = """
            UPDATE settlement_plan
            SET status = 'STALE', updated_at = now()
            WHERE group_id = :groupId AND currency_id = :currencyId AND status = 'ACTIVE'
            """, nativeQuery = true)
    int markStaleIfActive(@Param("groupId") UUID groupId, @Param("currencyId") UUID currencyId);

    /**
     * Vacate the live slot before a regeneration inserts the new ACTIVE plan.
     * Must run in the same transaction as that insert (the partial index covers
     * STALE too, so the old plan has to leave the slot first).
     */
    @Modifying
    @Query(value = """
            UPDATE settlement_plan
            SET status = 'SUPERSEDED', superseded_by = :newPlanId, updated_at = now()
            WHERE id = :oldPlanId AND status IN ('ACTIVE', 'STALE')
            """, nativeQuery = true)
    int supersede(@Param("oldPlanId") UUID oldPlanId, @Param("newPlanId") UUID newPlanId);

    /** Complete the plan once every line is fully CONFIRMED-fulfilled. ACTIVE only. */
    @Modifying
    @Query(value = """
            UPDATE settlement_plan
            SET status = 'COMPLETED', updated_at = now()
            WHERE id = :planId AND status = 'ACTIVE'
            """, nativeQuery = true)
    int markCompletedIfActive(@Param("planId") UUID planId);

    /** User abandons the current plan (whether ACTIVE or STALE). */
    @Modifying
    @Query(value = """
            UPDATE settlement_plan
            SET status = 'CANCELLED', updated_at = now()
            WHERE id = :planId AND status IN ('ACTIVE', 'STALE')
            """, nativeQuery = true)
    int cancelIfLive(@Param("planId") UUID planId);
}
