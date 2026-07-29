package com.vyay.core.repository.projection;

import java.util.UUID;

/**
 * Per-line derived progress for a settlement plan, summed from the settlements
 * linked to each line — never stored on the line itself.
 *
 * fulfilledMinor = SUM of CONFIRMED linked settlements (reduces the outstanding
 * amount and advances completion).
 * pendingMinor   = SUM of PROPOSED linked settlements (in-flight only; does NOT
 * reduce the outstanding amount until confirmed).
 *
 * Rejected/cancelled settlements fall into neither bucket, so they naturally
 * disappear from both sums with no unwind. The service derives workflow progress
 * from these values (e.g. remaining to execute = amount - fulfilled - pending),
 * while the true unsettled amount remains (amount - fulfilled).
 */
public interface PlanLineProgressView {
    UUID getLineId();
    UUID getFromUserId();
    UUID getToUserId();
    long getAmountMinor();
    long getFulfilledMinor();
    long getPendingMinor();
}
