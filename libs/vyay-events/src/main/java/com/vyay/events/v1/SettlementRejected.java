package com.vyay.events.v1;

import java.util.UUID;

/**
 * The payee declined a proposed settlement.
 *
 * Nothing unwinds: no balance ever moved, so there is nothing to reverse. The
 * amount is repeated so a feed row can say what was declined without joining back
 * to the proposal.
 *
 * {@code envelope().actorUserId()} is the payee — only they can reject.
 *
 * @param settlementId which settlement.
 * @param fromUserId   the proposed payer.
 * @param toUserId     the proposed payee, and the actor here.
 * @param amountMinor  amount in MINOR units of {@code currencyCode}.
 * @param currencyCode ISO code.
 */
public record SettlementRejected(
        EventEnvelope envelope,
        UUID settlementId,
        UUID fromUserId,
        UUID toUserId,
        long amountMinor,
        String currencyCode) implements GroupEvent {

    public static final String TYPE = "settlement.rejected.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
