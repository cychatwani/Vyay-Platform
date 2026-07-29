-- =====================================================================
-- V5__user_balance_totals.sql
-- Per-user, per-currency rollup of the balances table.
--
-- One row per (user, currency) holding the user's aggregate position across
-- ALL groups, so /me/balances is a single indexed read instead of a scan +
-- group-by over balances. Maintained inside the same transaction that writes
-- balances (see BalanceUpdateService), never by a background job, so it is
-- always consistent with SUM(balances) at commit time.
--
-- Sign convention (be explicit):
--   balances.net_amount_minor > 0  => others owe THIS user (a credit)
--   balances.net_amount_minor < 0  => THIS user owes others (a debt)
--
--   total_owed_minor   = SUM(net_amount_minor) over positive balances
--                        (money owed TO the user), stored positive.
--   total_owing_minor  = SUM(-net_amount_minor) over negative balances
--                        (money the user OWES), stored as a positive magnitude.
--
--   net = total_owed_minor - total_owing_minor.
--
-- currency_code is the Currency PK (uuid), NOT the ISO code — same historical
-- naming carried over from the balances table.
-- =====================================================================

CREATE TABLE user_balance_totals (
    user_id           uuid        NOT NULL,
    currency_code     uuid        NOT NULL,
    total_owed_minor  bigint      NOT NULL DEFAULT 0,
    total_owing_minor bigint      NOT NULL DEFAULT 0,
    updated_at        timestamptz NOT NULL,
    version           bigint,
    PRIMARY KEY (user_id, currency_code)
);

-- The PK's leading user_id column already covers the /me/balances lookup
-- (WHERE user_id = ?), so no additional index is needed for the read path.

ALTER TABLE user_balance_totals
    ADD CONSTRAINT fk_user_balance_totals_user     FOREIGN KEY (user_id)       REFERENCES users,
    ADD CONSTRAINT fk_user_balance_totals_currency FOREIGN KEY (currency_code) REFERENCES currencies;
