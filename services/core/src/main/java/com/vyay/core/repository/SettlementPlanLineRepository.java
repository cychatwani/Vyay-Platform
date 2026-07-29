package com.vyay.core.repository;

import com.vyay.core.entity.settlement.SettlementPlanLine;
import com.vyay.core.repository.projection.PlanLineProgressView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementPlanLineRepository extends JpaRepository<SettlementPlanLine, UUID> {

    List<SettlementPlanLine> findByPlanId(UUID planId);

    /**
     * The line a confirmed settlement reconciles against: same plan, same
     * direction (debtor -> creditor). Unique per plan, so Optional.
     */
    Optional<SettlementPlanLine> findByPlanIdAndFromUserIdAndToUserId(
            UUID planId, UUID fromUserId, UUID toUserId);

    /**
     * Per-line derived progress for one plan. LEFT JOINs so lines with no linked
     * settlements still return (fulfilled = pending = 0). FILTER splits the linked
     * settlements by status; soft-deleted settlements are excluded. Ordered by the
     * same (amount desc, from asc) tiebreak the generator uses, so the read model
     * matches generation order.
     */
    @Query(value = """
            SELECT l.id           AS lineId,
                   l.from_user_id AS fromUserId,
                   l.to_user_id   AS toUserId,
                   l.amount_minor AS amountMinor,
                   COALESCE(SUM(s.amount_minor) FILTER (WHERE s.status = 'CONFIRMED'), 0) AS fulfilledMinor,
                   COALESCE(SUM(s.amount_minor) FILTER (WHERE s.status = 'PROPOSED'),  0) AS pendingMinor
            FROM settlement_plan_line l
            LEFT JOIN settlement_plan_line_fulfillment f ON f.plan_line_id = l.id
            LEFT JOIN settlements s ON s.id = f.settlement_id AND s.deleted_at IS NULL
            WHERE l.plan_id = :planId
            GROUP BY l.id, l.from_user_id, l.to_user_id, l.amount_minor
            ORDER BY l.amount_minor DESC, l.from_user_id ASC
            """, nativeQuery = true)
    List<PlanLineProgressView> findProgressByPlanId(@Param("planId") UUID planId);

    /**
     * Count of lines not yet fully fulfilled by CONFIRMED settlements. Zero means
     * every line is settled and the plan can be marked COMPLETED. Only CONFIRMED
     * counts toward completion — PROPOSED is in-flight and inert.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM settlement_plan_line l
            WHERE l.plan_id = :planId
              AND l.amount_minor > COALESCE((
                    SELECT SUM(s.amount_minor)
                    FROM settlement_plan_line_fulfillment f
                    JOIN settlements s ON s.id = f.settlement_id
                    WHERE f.plan_line_id = l.id
                      AND s.status = 'CONFIRMED'
                      AND s.deleted_at IS NULL
                  ), 0)
            """, nativeQuery = true)
    long countUnfulfilledLines(@Param("planId") UUID planId);
}
