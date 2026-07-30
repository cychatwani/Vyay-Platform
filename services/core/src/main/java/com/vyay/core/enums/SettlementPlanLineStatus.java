package com.vyay.core.enums;

/**
 * Derived progress of a plan line — never stored. Computed from CONFIRMED
 * fulfilment vs the line amount: PENDING (nothing confirmed), COMPLETED (fully
 * confirmed), PARTIALLY_FULFILLED (in between). PROPOSED settlements are shown as
 * "pending" money in the read model but do NOT advance this status.
 */
public enum SettlementPlanLineStatus {
    PENDING,
    PARTIALLY_FULFILLED,
    COMPLETED
}
