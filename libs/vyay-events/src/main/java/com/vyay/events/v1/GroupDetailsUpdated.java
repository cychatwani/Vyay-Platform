package com.vyay.events.v1;

import java.util.List;

/**
 * A group's details were edited.
 *
 * NO PRODUCER IN CORE YET — defined ahead of one for the reason given on
 * {@link MemberRoleChanged}.
 *
 * Carries only what changed, unlike {@link ExpenseUpdated} which also repeats full
 * state. A group has few fields and they are all in {@link GroupCreated}, so a
 * projection can apply changes in order without a snapshot; an expense's payers and
 * shares are a collection whose new state cannot be expressed as field diffs.
 *
 * One user action is ONE event. Renaming a group and rewriting its description in
 * a single save produces a single event with two entries here — not two events,
 * which would render as two feed rows indistinguishable from two separate edits.
 *
 * @param changedFields what this edit touched, before and after. Never empty.
 *                      Field names are the domain names: {@code "name"},
 *                      {@code "description"}, {@code "type"},
 *                      {@code "defaultCurrencyCode"}.
 */
public record GroupDetailsUpdated(
        EventEnvelope envelope,
        List<FieldChange> changedFields) implements GroupEvent {

    public static final String TYPE = "group.details_updated.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
