package com.vyay.events.v1;

import java.time.Instant;
import java.util.UUID;

/**
 * A settlement was accepted and balances moved.
 *
 * The only settlement event that changes what anyone owes. A consumer tracking
 * balances should act on this one and ignore the other three.
 *
 * It does not always follow a {@link SettlementProposed}: a creditor recording
 * "they paid me" is asserting receipt, and a group with auto-settle confirms on
 * creation, so both land here directly with no proposal ever published.
 *
 * @param settlementId which settlement.
 * @param fromUserId   the payer (debtor).
 * @param toUserId     the payee (creditor).
 * @param amountMinor  amount in MINOR units of {@code currencyCode}. Always positive.
 * @param currencyCode ISO code.
 * @param confirmedAt  when it was accepted. Equal to {@code envelope().occurredAt()}
 *                     for a confirmation, and kept separately because it is the
 *                     settlement's own domain field rather than the event's.
 */
public record SettlementConfirmed(
        EventEnvelope envelope,
        UUID settlementId,
        UUID fromUserId,
        UUID toUserId,
        long amountMinor,
        String currencyCode,
        Instant confirmedAt) implements GroupEvent {

    public static final String TYPE = "settlement.confirmed.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
