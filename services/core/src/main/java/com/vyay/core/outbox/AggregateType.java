package com.vyay.core.outbox;

/**
 * Which sealed DomainEvent branch an outbox row came from. Mirrors the CHECK on
 * outbox_event.aggregate_type — a constant added here needs a migration widening
 * that constraint in the same change, or writes of the new kind fail at INSERT.
 */
public enum AggregateType {
    GROUP,
    USER
}
