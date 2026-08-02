package com.vyay.core.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Stand-in transport until Kafka lands: records that an event would have gone out and
 * reports success, so the relay settles the row PUBLISHED and the pipeline is testable
 * end to end without a broker.
 * Identifiers only — payloads carry user financial data and must not reach app logs.
 * No stereotype annotation: chunk 5 registers it conditionally, so Kafka displaces it.
 */
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public CompletableFuture<Void> publish(PublishableEvent event) {
        log.info("Outbox publish (logging): eventId={} eventType={} aggregate={}:{} occurredAt={}",
                event.eventId(), event.eventType(), event.aggregateType(),
                event.aggregateId(), event.occurredAt());
        return CompletableFuture.completedFuture(null);
    }
}
