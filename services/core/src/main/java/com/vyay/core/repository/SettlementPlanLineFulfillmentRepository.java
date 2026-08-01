package com.vyay.core.repository;

import com.vyay.core.entity.settlement.SettlementPlanLineFulfillment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface SettlementPlanLineFulfillmentRepository
        extends JpaRepository<SettlementPlanLineFulfillment, UUID> {

    /**
     * Guards reconciliation against double-linking a settlement (the unique
     * settlement_id constraint is the hard backstop; this makes the check cheap
     * and explicit before an insert).
     */
    boolean existsBySettlementId(UUID settlementId);

    /**
     * Drop every link belonging to the given settlements, in one statement.
     * <p>
     * Used by plan generation to carry PROPOSED settlements onto the new plan.
     * Generation rebuilds rather than re-points those links, because a settlement
     * can arrive with zero of them (proposed while no plan existed), one (the
     * ordinary case), or several (partially applied across lines of an earlier
     * plan) — deleting first collapses all three to the same end state: exactly one
     * link, at the settlement's full amount, on its carried line.
     * <p>
     * Callers MUST restrict the id list to non-CONFIRMED settlements. Deleting a
     * CONFIRMED settlement's links would silently un-fulfil a completed plan line,
     * since line progress is derived entirely from this table.
     * <p>
     * Bulk JPQL, so it bypasses the persistence context — safe here because
     * generation holds none of these entities.
     */
    @Modifying
    @Query("delete from SettlementPlanLineFulfillment f where f.settlement.id in :settlementIds")
    int deleteBySettlementIdIn(@Param("settlementIds") Collection<UUID> settlementIds);
}
