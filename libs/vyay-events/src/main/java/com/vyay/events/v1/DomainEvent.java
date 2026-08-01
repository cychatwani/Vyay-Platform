package com.vyay.events.v1;

/**
 * A fact that has already happened in the Core domain.
 *
 * Past tense throughout, and never a command or a request: an event says what was
 * done, so a consumer may reject it, ignore it, or replay it, but cannot refuse
 * it. By the time one is published the write it describes is committed.
 *
 * Sealed into exactly two branches, which is the whole reason the hierarchy
 * exists: {@link GroupEvent} always has a group, {@link UserEvent} never does.
 *
 * <pre>{@code
 * switch (event) {
 *     case GroupEvent g -> feed.append(g.groupId(), g);
 *     case UserEvent  u -> userProjection.apply(u);
 * }
 * }</pre>
 */
public sealed interface DomainEvent permits GroupEvent, UserEvent {

    /** Identity, ordering and attribution. Never null. */
    EventEnvelope envelope();

    /**
     * The wire discriminator: {@code "expense.created.v1"}.
     *
     * Constant per type and versioned, so it is what belongs in a message header
     * or a JSON {@code type} field — a package name cannot travel, and the class
     * name alone would lose the version. Each implementation also exposes it as a
     * {@code public static final String TYPE}, so a consumer can switch on the
     * string without instantiating anything.
     */
    String eventType();
}
