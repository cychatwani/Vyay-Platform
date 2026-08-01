package com.vyay.events.v1;

import java.util.UUID;

/**
 * Somebody became an active member of a group.
 *
 * Covers a first join and a rejoin alike — a member who left and came back is
 * active again, and that is the same fact for a feed. A consumer needing to tell
 * them apart can, since it has seen the {@link MemberLeft} in between.
 *
 * {@code envelope().actorUserId()} is normally the joiner themselves. It differs
 * when someone is added by another member, in which case the actor is the adder
 * and {@code memberUserId} is the person added.
 *
 * @param memberUserId who is now a member.
 * @param role         what they joined as.
 */
public record MemberJoined(
        EventEnvelope envelope,
        UUID memberUserId,
        GroupRole role) implements GroupEvent {

    public static final String TYPE = "member.joined.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
