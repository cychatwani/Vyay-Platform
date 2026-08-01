package com.vyay.events.v1;

import java.util.UUID;

/**
 * A member was removed from a group by somebody else.
 *
 * {@code envelope().actorUserId()} is who removed them and {@code memberUserId} is
 * who was removed; the two always differ, which is the whole distinction from
 * {@link MemberLeft}. Both facts are needed to render the row honestly — "Priya
 * removed Arun" cannot be reconstructed from either id alone.
 *
 * @param memberUserId who was removed.
 */
public record MemberRemoved(
        EventEnvelope envelope,
        UUID memberUserId) implements GroupEvent {

    public static final String TYPE = "member.removed.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
