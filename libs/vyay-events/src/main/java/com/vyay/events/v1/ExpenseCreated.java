package com.vyay.events.v1;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An expense was added to a group.
 *
 * Carries the whole expense, payers and shares included, so a consumer can build
 * its projection from this event alone without calling back into Core. That is the
 * point of an event feed: a callback would couple the consumer to Core's
 * availability and would read the expense as it is NOW rather than as it was when
 * this happened.
 *
 * @param expenseId        the expense's own id.
 * @param description      what it was for. Domain data, not display data — a feed
 *                         row is unreadable without it.
 * @param totalAmountMinor the total, in MINOR units of {@code currencyCode}.
 * @param currencyCode     ISO code, e.g. {@code "INR"}. Never Core's internal
 *                         currency UUID; a consumer must not need Core's keys.
 * @param splitType        how the total was divided.
 * @param expenseDate      when the spend happened — set by the user, and often not
 *                         the day it was entered. Distinct from
 *                         {@code envelope().occurredAt()}, which is when the row
 *                         was written.
 * @param notes            free-text memo. Nullable.
 * @param payers           who actually paid, and how much each. Never empty.
 * @param shares           who owes what. Never empty.
 */
public record ExpenseCreated(
        EventEnvelope envelope,
        UUID expenseId,
        String description,
        long totalAmountMinor,
        String currencyCode,
        SplitType splitType,
        Instant expenseDate,
        String notes,
        List<PayerAmount> payers,
        List<ShareAmount> shares) implements GroupEvent {

    public static final String TYPE = "expense.created.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
