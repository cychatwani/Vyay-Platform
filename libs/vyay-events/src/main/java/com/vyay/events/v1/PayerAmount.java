package com.vyay.events.v1;

import java.util.UUID;

/**
 * How much one person actually put in towards an expense.
 *
 * An expense may have several payers, so this is a list on the event rather than a
 * single {@code paidByUserId}.
 *
 * @param userId          who paid. An id only — a consumer resolves the name from
 *                        its own member store.
 * @param amountPaidMinor what they paid, in the expense's currency, in MINOR units
 *                        (paise, cents). Always positive.
 */
public record PayerAmount(UUID userId, long amountPaidMinor) {
}
