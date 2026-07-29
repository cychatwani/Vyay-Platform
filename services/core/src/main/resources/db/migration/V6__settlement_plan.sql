-- =====================================================================
-- V6__settlement_plan.sql
-- Persisted settle-up plans: a group workflow with identity and a lifecycle,
-- not a stateless suggestion endpoint. A plan nets a group's balances (for one
-- currency) down to <= n-1 directed transfers that members execute over days.
--
-- Three tables:
--   settlement_plan                  — the aggregate: one live plan per
--                                      (group, currency), with a stored status.
--   settlement_plan_line             — a directed transfer (from -> to, amount)
--                                      the plan proposes. Immutable. NO status
--                                      column: line progress is DERIVED from the
--                                      settlements linked to it, never stored.
--   settlement_plan_line_fulfillment — the link between a plan line and the
--                                      real settlements that (partially) fulfil
--                                      it. Fulfilment lives entirely in this
--                                      join, so nothing can drift from the
--                                      settlements table and a rejected/undone
--                                      settlement simply drops out of the sums.
--
-- Why status is STORED on the plan but DERIVED on the line:
--   * Line progress (PENDING / PARTIALLY_FULFILLED / COMPLETED) is a pure
--     function of SUM(CONFIRMED settlements) vs the line amount — always
--     recomputable, so storing it would only invite drift.
--   * Plan status carries facts that are NOT derivable from balances or lines —
--     STALE (balances moved out from under the plan) and SUPERSEDED (replaced by
--     a regeneration) — and the "one live plan" partial index below needs a
--     concrete column to filter on. So the plan keeps a stored status column.
--
-- Note on currency_id: existing tables (balances, settlements) named this column
-- currency_code for historical reasons even though it holds the Currency UUID
-- PK. These new tables use the honest name currency_id (same type, a uuid FK to
-- currencies).
-- =====================================================================

CREATE TYPE settlement_plan_status AS ENUM ('ACTIVE', 'STALE', 'SUPERSEDED', 'COMPLETED', 'CANCELLED');

-- ---------------------------------------------------------------------
-- settlement_plan — the aggregate
-- ---------------------------------------------------------------------
CREATE TABLE settlement_plan (
    id            uuid                   NOT NULL,
    group_id      uuid                   NOT NULL,
    currency_id   uuid                   NOT NULL,
    created_by    uuid                   NOT NULL,
    status        settlement_plan_status NOT NULL,
    -- Traceability for the regenerate chain: set on the OLD plan when a new one
    -- supersedes it. Nullable; only ever points forward to the replacement.
    superseded_by uuid,
    created_at    timestamptz            NOT NULL,
    updated_at    timestamptz            NOT NULL,
    -- No version column: status is the only mutable field and every transition is
    -- a guarded compare-and-set UPDATE (WHERE status = expected) backed by the
    -- partial unique index below, so optimistic locking would add a retry loop on
    -- the staleness hot path without closing any race the CAS doesn't.
    PRIMARY KEY (id)
);

-- At most ONE live plan per (group, currency). "Live" = ACTIVE or STALE; a STALE
-- plan is still the current one (read-only, awaiting regeneration), so it must
-- keep occupying the slot. COMPLETED / CANCELLED / SUPERSEDED plans are history
-- and fall out of the index, so a fresh plan can take the slot.
--
-- This is why regeneration MUST, in one transaction, first move the old plan to
-- SUPERSEDED (vacating the slot) and only then insert the new ACTIVE plan.
CREATE UNIQUE INDEX uk_settlement_plan_live_group_currency
    ON settlement_plan (group_id, currency_id)
    WHERE status IN ('ACTIVE', 'STALE');

-- ---------------------------------------------------------------------
-- settlement_plan_line — a proposed directed transfer (immutable)
-- ---------------------------------------------------------------------
CREATE TABLE settlement_plan_line (
    id           uuid        NOT NULL,
    plan_id      uuid        NOT NULL,
    from_user_id uuid        NOT NULL,   -- debtor: pays
    to_user_id   uuid        NOT NULL,   -- creditor: receives
    amount_minor bigint      NOT NULL,
    created_at   timestamptz NOT NULL,
    PRIMARY KEY (id),
    -- One directed edge per ordered pair within a plan. This is also the row
    -- reconciliation matches a confirmed settlement against (plan_id, from, to).
    CONSTRAINT uk_settlement_plan_line_plan_from_to UNIQUE (plan_id, from_user_id, to_user_id),
    CONSTRAINT ck_settlement_plan_line_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT ck_settlement_plan_line_distinct_parties CHECK (from_user_id <> to_user_id)
);

-- ---------------------------------------------------------------------
-- settlement_plan_line_fulfillment — line <-> settlement link (derived progress)
-- ---------------------------------------------------------------------
CREATE TABLE settlement_plan_line_fulfillment (
    id            uuid        NOT NULL,
    plan_line_id  uuid        NOT NULL,
    settlement_id uuid        NOT NULL,
    created_at    timestamptz NOT NULL,
    PRIMARY KEY (id),
    -- A settlement fulfils AT MOST ONE line: unique on settlement_id.
    CONSTRAINT uk_settlement_plan_line_fulfillment_settlement UNIQUE (settlement_id)
    -- plan_line_id is deliberately NOT unique: one line may be fulfilled by
    -- several settlements (partial fulfilment).
);

-- Sum fulfilments per line (the derived-progress read path); plan_line_id is
-- non-unique so it needs its own index.
CREATE INDEX idx_settlement_plan_line_fulfillment_line
    ON settlement_plan_line_fulfillment (plan_line_id);

-- ---------------------------------------------------------------------
-- Foreign keys
-- ---------------------------------------------------------------------
ALTER TABLE settlement_plan
    ADD CONSTRAINT fk_settlement_plan_group      FOREIGN KEY (group_id)      REFERENCES groups,
    ADD CONSTRAINT fk_settlement_plan_currency   FOREIGN KEY (currency_id)   REFERENCES currencies,
    ADD CONSTRAINT fk_settlement_plan_created_by FOREIGN KEY (created_by)    REFERENCES users,
    ADD CONSTRAINT fk_settlement_plan_superseded FOREIGN KEY (superseded_by) REFERENCES settlement_plan;

ALTER TABLE settlement_plan_line
    ADD CONSTRAINT fk_settlement_plan_line_plan FOREIGN KEY (plan_id)      REFERENCES settlement_plan,
    ADD CONSTRAINT fk_settlement_plan_line_from FOREIGN KEY (from_user_id) REFERENCES users,
    ADD CONSTRAINT fk_settlement_plan_line_to   FOREIGN KEY (to_user_id)   REFERENCES users;

ALTER TABLE settlement_plan_line_fulfillment
    ADD CONSTRAINT fk_settlement_plan_line_fulfillment_line       FOREIGN KEY (plan_line_id)  REFERENCES settlement_plan_line,
    ADD CONSTRAINT fk_settlement_plan_line_fulfillment_settlement FOREIGN KEY (settlement_id) REFERENCES settlements;
