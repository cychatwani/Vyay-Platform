package com.vyay.events.v1;

import java.util.UUID;

/**
 * A member left a group of their own accord.
 *
 * {@code memberUserId} always equals {@code envelope().actorUserId()} here — that
 * is exactly what separates this from {@link MemberRemoved}. It is still stated
 * explicitly so a consumer can read one field for "who is no longer a member"
 * across both events instead of branching on the type.
 *
 * A departing member's balances do not vanish with them: Core blocks leaving with
 * an outstanding balance, and past expenses keep referencing them.
 *
 * @param memberUserId who left.
 */
public record MemberLeft(
        EventEnvelope envelope,
        UUID memberUserId) implements GroupEvent {

    public static final String TYPE = "member.left.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
