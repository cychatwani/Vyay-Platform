package com.vyay.core.repository;

import com.vyay.core.entity.settlement.SettlementPlanLine;
import com.vyay.core.repository.projection.PlanLineProgressView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SettlementPlanLineRepository extends JpaRepository<SettlementPlanLine, UUID> {

    List<SettlementPlanLine> findByPlanId(UUID planId);

    /**
     * The lines a confirmed settlement reconciles against: same plan, same
     * direction (debtor -> creditor). No longer unique — the append model allows
     * several lines per pair — so this returns a List, oldest-first by id (UUIDv7
     * is time-ordered), which is the order reconciliation fills them in.
     */
    @Query("""
            select l from SettlementPlanLine l
            where l.plan.id = :planId
              and l.fromUser.id = :fromUserId
              and l.toUser.id = :toUserId
            order by l.id asc
            """)
    List<SettlementPlanLine> findByPlanIdAndFromUserIdAndToUserId(
            @Param("planId") UUID planId,
            @Param("fromUserId") UUID fromUserId,
            @Param("toUserId") UUID toUserId);

    /**
     * Per-line derived progress for one plan. LEFT JOINs so lines with no linked
     * settlements still return (fulfilled = pending = 0). Sums the per-link
     * applied_amount_minor (NOT the settlement's own amount — one settlement may
     * apply different amounts to different lines), split by the settlement's
     * status; soft-deleted settlements are excluded. Ordered chronologically so
     * appended lines read as additions at the bottom rather than scattering into
     * the middle; id breaks ties within a generation batch.
     */
    @Query(value = """
            SELECT l.id           AS lineId,
                   l.from_user_id AS fromUserId,
                   l.to_user_id   AS toUserId,
                   l.amount_minor AS amountMinor,
                   l.created_at   AS createdAt,
                   COALESCE(SUM(f.applied_amount_minor) FILTER (WHERE s.status = 'CONFIRMED'), 0) AS fulfilledMinor,
                   COALESCE(SUM(f.applied_amount_minor) FILTER (WHERE s.status = 'PROPOSED'),  0) AS pendingMinor
            FROM settlement_plan_line l
            LEFT JOIN settlement_plan_line_fulfillment f ON f.plan_line_id = l.id
            LEFT JOIN settlements s ON s.id = f.settlement_id AND s.deleted_at IS NULL
            WHERE l.plan_id = :planId
            GROUP BY l.id, l.from_user_id, l.to_user_id, l.amount_minor, l.created_at
            ORDER BY l.created_at ASC, l.id ASC
            """, nativeQuery = true)
    List<PlanLineProgressView> findProgressByPlanId(@Param("planId") UUID planId);

    /**
     * Count of lines not yet fully fulfilled by CONFIRMED settlements. Zero means
     * every line is settled and the plan can be marked COMPLETED. Only CONFIRMED
     * counts toward completion — PROPOSED is in-flight and inert. Sums the
     * per-link applied_amount_minor, not the settlement's own amount.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM settlement_plan_line l
            WHERE l.plan_id = :planId
              AND l.amount_minor > COALESCE((
                    SELECT SUM(f.applied_amount_minor)
                    FROM settlement_plan_line_fulfillment f
                    JOIN settlements s ON s.id = f.settlement_id
                    WHERE f.plan_line_id = l.id
                      AND s.status = 'CONFIRMED'
                      AND s.deleted_at IS NULL
                  ), 0)
            """, nativeQuery = true)
    long countUnfulfilledLines(@Param("planId") UUID planId);
}
