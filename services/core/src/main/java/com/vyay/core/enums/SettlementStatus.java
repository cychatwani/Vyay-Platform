package com.vyay.core.enums;

public enum SettlementStatus {
    PROPOSED,    // recorded; balances NOT yet moved
    CONFIRMED,   // accepted -> balance deltas applied
    CANCELLED,   // initiator withdrew it before confirmation
    REJECTED     // counterparty declined it
}