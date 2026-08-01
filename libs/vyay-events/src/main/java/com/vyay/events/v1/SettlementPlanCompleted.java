package com.vyay.events.v1;

import java.util.UUID;

/**
 * Every line of a settle-up plan has been fulfilled.
 *
 * COMPLETED means the plan finished, NOT that the group is square. Balances can
 * have moved while the plan was being executed — a forgotten expense entered
 * halfway through — so the last line can be paid while the group still owes.
 * {@code residualAmountMinor} is what separates those two facts, and without it on
 * the event a consumer would have to call Core to find out which one to announce.
 *
 * @param planId               which plan finished.
 * @param currencyCode         ISO code.
 * @param residualAmountMinor  what is still owed in this currency once the plan is
 *                             done, as the total of all outstanding debt in MINOR
 *                             units. ZERO means the group is genuinely settled and
 *                             the consumer may say so; anything above zero means
 *                             the plan completed with a gap and the group is not
 *                             square. Never negative.
 */
public record SettlementPlanCompleted(
        EventEnvelope envelope,
        UUID planId,
        String currencyCode,
        long residualAmountMinor) implements GroupEvent {

    public static final String TYPE = "settlement_plan.completed.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
