package com.vyay.events.v1;

/**
 * A group was created.
 *
 * The first event in any group's stream, and the one a consumer's group projection
 * is seeded from. The creator is {@code envelope().actorUserId()}; their membership
 * arrives as a separate {@link MemberJoined} rather than being implied here, so a
 * consumer has exactly one way to learn that somebody is a member.
 *
 * @param name                 the group's name.
 * @param description          nullable.
 * @param type                 what the group is for.
 * @param defaultCurrencyCode  ISO code used for expenses that do not name one.
 */
public record GroupCreated(
        EventEnvelope envelope,
        String name,
        String description,
        GroupType type,
        String defaultCurrencyCode) implements GroupEvent {

    public static final String TYPE = "group.created.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
