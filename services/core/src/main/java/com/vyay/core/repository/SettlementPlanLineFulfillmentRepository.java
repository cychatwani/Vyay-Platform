package com.vyay.core.repository;

import com.vyay.core.entity.settlement.SettlementPlanLineFulfillment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SettlementPlanLineFulfillmentRepository
        extends JpaRepository<SettlementPlanLineFulfillment, UUID> {

    /**
     * Guards reconciliation against double-linking a settlement (the unique
     * settlement_id constraint is the hard backstop; this makes the check cheap
     * and explicit before an insert).
     */
    boolean existsBySettlementId(UUID settlementId);
}
