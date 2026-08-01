package com.vyay.events.v1;

import java.util.UUID;

/**
 * A settle-up plan was generated for a group and currency.
 *
 * Summary only — the lines are not carried. A plan is an execution workflow whose
 * lines change as it runs (they are appended to, and fulfilled against), so a copy
 * taken at generation would be stale almost immediately and would tempt a consumer
 * into treating the event as the plan's state. The count and total are enough for
 * a feed row; anything more belongs to Core's plan API.
 *
 * Generate doubles as regenerate, so this also fires when a plan replaces an
 * existing one. {@code supersededPlanId} is how the two cases are told apart.
 *
 * @param planId            the new plan's id.
 * @param currencyCode      ISO code — a plan settles exactly one currency.
 * @param lineCount         how many transfers the plan proposes.
 * @param totalAmountMinor  the sum of every line's amount, in MINOR units. Note
 *                          this is the money to be MOVED, which exceeds the sum of
 *                          debts only in the sense that netting has already
 *                          minimised it.
 * @param supersededPlanId  the plan this one replaced, or NULL if the group had no
 *                          live plan — i.e. null means a first generation, non-null
 *                          means a regeneration.
 */
public record SettlementPlanGenerated(
        EventEnvelope envelope,
        UUID planId,
        String currencyCode,
        int lineCount,
        long totalAmountMinor,
        UUID supersededPlanId) implements GroupEvent {

    public static final String TYPE = "settlement_plan.generated.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
