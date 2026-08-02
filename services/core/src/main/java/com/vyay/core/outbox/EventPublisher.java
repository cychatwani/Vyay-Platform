package com.vyay.core.outbox;

import java.util.concurrent.CompletableFuture;

/**
 * Transport for outbox events. The topic is NOT part of this interface and is not a
 * column: an implementation derives it from aggregateType through its own config, so
 * retopicing stays a config change instead of a data migration.
 * Implementations must be thread-safe — the relay fires a whole batch before awaiting
 * any of it — and delivery is at-least-once; consumers dedupe on eventId.
 */
public interface EventPublisher {

    /**
     * Never throws synchronously: a failure arrives as a failed future, so the relay
     * can split one batch into succeeded and failed without a try/catch per send.
     */
    CompletableFuture<Void> publish(PublishableEvent event);
}
