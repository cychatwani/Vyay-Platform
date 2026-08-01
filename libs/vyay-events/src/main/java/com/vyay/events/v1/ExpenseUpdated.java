package com.vyay.events.v1;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An existing expense was edited.
 *
 * Carries both the complete NEW state and a list of what changed, which is
 * redundant on purpose. The full state lets a projection overwrite its row without
 * having held the previous version — which matters for a consumer that was added
 * late, or that lost its store and is replaying from the start of the topic. The
 * change list is what a feed row needs to say "changed the amount from ₹800 to
 * ₹1,200" rather than the useless "edited an expense".
 *
 * Every field below mirrors {@link ExpenseCreated} and holds the value AFTER the
 * edit.
 *
 * @param changedFields exactly the fields this edit touched, with before and after
 *                      values. Never empty — an edit that changed nothing does not
 *                      produce an event.
 */
public record ExpenseUpdated(
        EventEnvelope envelope,
        UUID expenseId,
        String description,
        long totalAmountMinor,
        String currencyCode,
        SplitType splitType,
        Instant expenseDate,
        String notes,
        List<PayerAmount> payers,
        List<ShareAmount> shares,
        List<FieldChange> changedFields) implements GroupEvent {

    public static final String TYPE = "expense.updated.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
