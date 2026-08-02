-- V8__outbox_event.sql
-- Transactional outbox: the event row commits with the domain write, a relay
-- publishes it after. Removes the gap where a crash loses an event.
-- Delivery is at-least-once, so consumers dedupe on id.
-- A row's status is the whole of the relay's progress — no state lives elsewhere.

CREATE TYPE outbox_status AS ENUM ('PENDING', 'IN_FLIGHT', 'PUBLISHED', 'DEAD');

CREATE TABLE outbox_event (
    -- EventEnvelope.eventId, caller-assigned: a DB default would mint a second id
    -- the payload disagrees with, and at-least-once guarantees someone meets it.
    id              uuid          NOT NULL,
    aggregate_type  text          NOT NULL,
    aggregate_id    uuid          NOT NULL,    -- Kafka key; per-key is the only ordering
    event_type      text          NOT NULL,    -- e.g. 'expense.created.v1'
    payload         jsonb         NOT NULL,
    occurred_at     timestamptz   NOT NULL,    -- domain time from the envelope, never now()
    status          outbox_status NOT NULL DEFAULT 'PENDING',
    attempt_count   integer       NOT NULL DEFAULT 0,
    claimed_at      timestamptz,               -- reaper frees claims older than claim-timeout
    claimed_by      text,                      -- forensics; SKIP LOCKED already gives exclusivity
    published_at    timestamptz,
    last_error      text,
    created_at      timestamptz   NOT NULL DEFAULT now(),

    PRIMARY KEY (id),
    CONSTRAINT ck_outbox_event_aggregate_type CHECK (aggregate_type IN ('GROUP', 'USER')),
    CONSTRAINT ck_outbox_event_attempt_count_non_negative CHECK (attempt_count >= 0)
);

-- No foreign keys, alone in V1-V8. Events outlive their aggregates (ExpenseDeleted,
-- MemberRemoved), and an FK would lock `groups` inside every domain write.

-- No watermark cursor: positions are assigned at INSERT but rows appear at COMMIT,
-- so a slow transaction is overtaken and skipped forever, PENDING with no error.

-- Claim scan, oldest first. Partial as PUBLISHED soon dominates; IN_FLIGHT shares it
-- with the reaper.
CREATE INDEX idx_outbox_event_unpublished
    ON outbox_event (occurred_at)
    WHERE status IN ('PENDING', 'IN_FLIGHT');

-- Purge job (later chunk): PUBLISHED rows past retention.
CREATE INDEX idx_outbox_event_published
    ON outbox_event (published_at)
    WHERE status = 'PUBLISHED';
