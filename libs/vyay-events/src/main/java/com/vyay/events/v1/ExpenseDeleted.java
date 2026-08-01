package com.vyay.events.v1;

import java.util.UUID;

/**
 * An expense was removed from a group.
 *
 * Repeats the description and total rather than sending only the id. A feed row
 * must read "deleted 'Dinner' ₹1,200" forever, and a consumer that joined after
 * the expense was created never saw those values — with an id alone the row would
 * degrade to "deleted an expense" for exactly the consumers most likely to be
 * replaying history.
 *
 * @param expenseId        which expense.
 * @param description      what it was for, as at deletion.
 * @param totalAmountMinor its total, in MINOR units of {@code currencyCode}.
 * @param currencyCode     ISO code.
 */
public record ExpenseDeleted(
        EventEnvelope envelope,
        UUID expenseId,
        String description,
        long totalAmountMinor,
        String currencyCode) implements GroupEvent {

    public static final String TYPE = "expense.deleted.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
