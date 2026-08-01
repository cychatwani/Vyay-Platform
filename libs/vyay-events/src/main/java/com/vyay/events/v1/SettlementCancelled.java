package com.vyay.events.v1;

import java.util.UUID;

/**
 * The payer withdrew a proposed settlement before it was answered.
 *
 * Distinct from {@link SettlementRejected} in who acted, not in what happened to
 * the money — in both cases nothing moved and nothing unwinds. The distinction is
 * kept because "they took it back" and "I declined it" are different facts to show
 * a user, and collapsing them into one event type would make that unrecoverable.
 *
 * {@code envelope().actorUserId()} is the payer — only they can cancel.
 *
 * @param settlementId which settlement.
 * @param fromUserId   the payer, and the actor here.
 * @param toUserId     the payee.
 * @param amountMinor  amount in MINOR units of {@code currencyCode}.
 * @param currencyCode ISO code.
 */
public record SettlementCancelled(
        EventEnvelope envelope,
        UUID settlementId,
        UUID fromUserId,
        UUID toUserId,
        long amountMinor,
        String currencyCode) implements GroupEvent {

    public static final String TYPE = "settlement.cancelled.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
