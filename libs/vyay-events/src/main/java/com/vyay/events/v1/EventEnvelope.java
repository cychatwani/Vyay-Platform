package com.vyay.events.v1;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The metadata every domain event carries, held as one component rather than
 * copied across eighteen records.
 *
 * @param eventId     UUIDv7, assigned once at write time. Time-ordered, so it
 *                    doubles as a tiebreaker for events sharing an
 *                    {@code occurredAt}, and stable enough to deduplicate on: a
 *                    redelivered event keeps the id it was written with.
 * @param occurredAt  when the fact happened in the domain — the timestamp of the
 *                    write that caused it, NOT when it was published. The
 *                    user-scoped activity feed merges several groups and orders
 *                    globally on this field; stamped at publish time it would
 *                    instead be ordered by the relay's poll interval, so a batch
 *                    of older events would surface above newer ones every time a
 *                    poll lagged.
 * @param groupId     the group this event belongs to. NULL for, and only for,
 *                    {@link UserEvent} — user lifecycle is not group-scoped. The
 *                    {@link GroupEvent} sub-interface exists so that distinction
 *                    is visible to the compiler rather than only in this comment.
 * @param actorUserId who performed the action. Always a real user: every event in
 *                    the catalogue is caused by somebody doing something, and
 *                    none is emitted by a scheduler or a background sweep.
 */
public record EventEnvelope(
        UUID eventId,
        Instant occurredAt,
        UUID groupId,
        UUID actorUserId) {

    /**
     * Rejects an envelope missing anything a v1 event must always have.
     *
     * Only these three are checked, and only because they are guaranteed present
     * for the whole life of v1 — a field added later must stay nullable, or an old
     * producer's events would stop deserialising. {@code groupId} is exempt by
     * design; see the parameter doc.
     */
    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(actorUserId, "actorUserId");
    }
}
