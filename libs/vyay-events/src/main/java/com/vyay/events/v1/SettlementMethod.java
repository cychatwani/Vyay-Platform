package com.vyay.events.v1;

/**
 * The channel a settlement's money moved through. Copied from the domain — see
 * {@link SplitType}.
 *
 * Nullable wherever it appears: the method is not always known at the time the
 * settlement is recorded.
 */
public enum SettlementMethod {
    UPI,
    CASH,
    OTHER
}
