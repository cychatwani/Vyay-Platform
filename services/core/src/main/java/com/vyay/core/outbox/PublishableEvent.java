package com.vyay.core.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One event handed to an {@link EventPublisher}. Deliberately NOT a persistence
 * type: a publisher never sees a JPA entity or a row shape, which is what lets the
 * Kafka implementation be written without touching the repository.
 * payload arrives already serialized — publishers transport bytes, they do not
 * serialize, so a redeploy cannot change the wire format of a recorded event.
 */
public record PublishableEvent(
        UUID eventId,                   // consumers dedupe on this
        AggregateType aggregateType,
        UUID aggregateId,               // the future Kafka message key
        String eventType,               // versioned, e.g. 'expense.created.v1'
        String payload,
        Instant occurredAt              // domain time from the envelope, never insert time
) {

    /**
     * Rejected here rather than at the publisher: a malformed event is a writer bug,
     * and the relay would otherwise retry it until it hit the attempt cap.
     */
    public PublishableEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        requireText(eventType, "eventType");
        requireText(payload, "payload");
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
