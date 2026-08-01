package com.vyay.events.v1;

import java.util.UUID;

/**
 * Somebody recorded a payment between two members that has not been accepted yet.
 *
 * No balance has moved. Balances shift only on confirmation, so a consumer must
 * not treat this as money changing hands — it is an assertion awaiting the
 * counterparty's answer, and {@link SettlementRejected} or
 * {@link SettlementCancelled} may follow instead of {@link SettlementConfirmed}.
 *
 * {@code envelope().actorUserId()} is whoever recorded it, which is not necessarily
 * either party: a third party may record on others' behalf where group policy
 * allows.
 *
 * @param settlementId the settlement's own id, the key every later settlement
 *                     event in this lifecycle repeats.
 * @param fromUserId   the payer (debtor).
 * @param toUserId     the payee (creditor).
 * @param amountMinor  amount in MINOR units of {@code currencyCode}. Always positive.
 * @param currencyCode ISO code.
 * @param method       how the money moved. Nullable — often not known when recorded.
 * @param note         free-text memo. Nullable.
 */
public record SettlementProposed(
        EventEnvelope envelope,
        UUID settlementId,
        UUID fromUserId,
        UUID toUserId,
        long amountMinor,
        String currencyCode,
        SettlementMethod method,
        String note) implements GroupEvent {

    public static final String TYPE = "settlement.proposed.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
