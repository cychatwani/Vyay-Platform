package com.vyay.core.enums;

/**
 * Lifecycle of a settlement plan. Stored (not derived), because STALE and
 * SUPERSEDED carry facts nothing else records, and the "one live plan per
 * (group, currency)" partial index needs a concrete column to filter on.
 *
 * The "live slot" — at most one plan per (group, currency) — is exactly
 * {ACTIVE, STALE}; the other three are terminal history and vacate the slot.
 */
public enum SettlementPlanStatus {
    ACTIVE,      // executable: lines currently match balances
    STALE,       // still the current plan, but balances moved — read-only, regenerate to refresh
    SUPERSEDED,  // history: replaced by a newer plan (see superseded_by)
    COMPLETED,   // every line fully fulfilled by CONFIRMED settlements
    CANCELLED    // user intentionally abandoned it before completion
}
