package com.vyay.events.v1;

import java.util.UUID;

/**
 * A member was promoted or demoted.
 *
 * NO PRODUCER IN CORE YET — there is no role-change endpoint. Defined now because
 * retrofitting an event type once consumers exist is expensive: every consumer has
 * to be told, and any history from before the type existed is unrecoverable. A
 * type with no producer costs nothing.
 *
 * Both roles are carried rather than just the new one, so a feed row reads
 * "made Arun an admin" without the consumer having to hold prior state.
 *
 * @param memberUserId whose role changed.
 * @param oldRole      what they were.
 * @param newRole      what they are now. Always differs from {@code oldRole}.
 */
public record MemberRoleChanged(
        EventEnvelope envelope,
        UUID memberUserId,
        GroupRole oldRole,
        GroupRole newRole) implements GroupEvent {

    public static final String TYPE = "member.role_changed.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
