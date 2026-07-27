-- =====================================================================
-- V1__initial_schema.sql
-- SplitEasyCore / Vyay — initial baseline schema
--
-- Derived from the Hibernate-generated DDL and enum source files.
-- Changes applied vs. raw Hibernate output:
--   * varchar + CHECK(...) columns backed by a Java enum are now native
--     PostgreSQL ENUM types (labels match Enum.name(), i.e. UPPERCASE).
--   * Hibernate-generated FK names (FK<hash>) replaced with fk_<table>_<ref>.
-- Everything else (tables, columns, nullability, PKs, unique constraints,
-- JSONB, UUID) is preserved exactly. timestamp(6) with time zone is
-- expressed as TIMESTAMPTZ (identical semantics, microsecond precision).
-- =====================================================================


-- ---------------------------------------------------------------------
-- Enum types
-- ---------------------------------------------------------------------
CREATE TYPE auth_provider      AS ENUM ('GOOGLE', 'APPLE', 'PASSWORD', 'REFRESH');
CREATE TYPE group_type         AS ENUM ('HOME', 'TRIP', 'COUPLE', 'OTHER', 'INDIVIDUAL');
CREATE TYPE group_role         AS ENUM ('ADMIN', 'MEMBER');
CREATE TYPE membership_status  AS ENUM ('ACTIVE', 'LEFT', 'REMOVED');
CREATE TYPE invite_link_type   AS ENUM ('PRIMARY', 'TEMPORARY');
CREATE TYPE ledger_source_type AS ENUM ('EXPENSE', 'SETTLEMENT');
CREATE TYPE split_type         AS ENUM ('EQUAL', 'EXACT', 'PERCENTAGE', 'SHARES');
CREATE TYPE settlement_method  AS ENUM ('UPI', 'CASH', 'OTHER');
CREATE TYPE settlement_status  AS ENUM ('PROPOSED', 'CONFIRMED', 'CANCELLED', 'REJECTED');


-- ---------------------------------------------------------------------
-- Reference / seed tables
-- ---------------------------------------------------------------------
CREATE TABLE currencies (
    code           varchar(3)  NOT NULL UNIQUE,
    decimal_places integer     NOT NULL,
    symbol         varchar(5)  NOT NULL,
    id             uuid        NOT NULL,
    name           varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE languages (
    code        varchar(5)   NOT NULL UNIQUE,
    id          uuid         NOT NULL,
    name        varchar(255) NOT NULL,
    native_name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE notification_templates (
    id      uuid        NOT NULL,
    channel varchar(20) NOT NULL,
    type    varchar(50) NOT NULL UNIQUE,
    body    TEXT        NOT NULL,
    subject varchar(255) NOT NULL,
    PRIMARY KEY (id)
);


-- ---------------------------------------------------------------------
-- Core tables
-- ---------------------------------------------------------------------
CREATE TABLE users (
    email_verified  boolean       NOT NULL,
    created_at      timestamptz   NOT NULL,
    deleted_at      timestamptz,
    updated_at      timestamptz   NOT NULL,
    version         bigint,
    id              uuid          NOT NULL,
    auth_provider   auth_provider NOT NULL,
    password_hash   varchar(100),
    email           varchar(255)  NOT NULL UNIQUE,
    first_name      varchar(255)  NOT NULL,
    full_name       varchar(255)  NOT NULL,
    last_name       varchar(255)  NOT NULL,
    profile_picture varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE user_profiles (
    created_at       timestamptz NOT NULL,
    updated_at       timestamptz NOT NULL,
    default_currency uuid        NOT NULL,
    id               uuid        NOT NULL,
    language         uuid        NOT NULL,
    preferences      jsonb,
    PRIMARY KEY (id)
);

CREATE TABLE groups (
    member_count     integer     NOT NULL,
    created_at       timestamptz NOT NULL,
    deleted_at       timestamptz,
    updated_at       timestamptz NOT NULL,
    version          bigint,
    created_by       uuid        NOT NULL,
    default_currency uuid        NOT NULL,
    id               uuid        NOT NULL,
    type             group_type  NOT NULL,
    description      varchar(255),
    name             varchar(255) NOT NULL,
    preferences      jsonb,
    PRIMARY KEY (id)
);

CREATE TABLE group_memberships (
    active_since timestamptz       NOT NULL,
    created_at   timestamptz       NOT NULL,
    deleted_at   timestamptz,
    left_at      timestamptz,
    updated_at   timestamptz       NOT NULL,
    version      bigint,
    group_id     uuid              NOT NULL,
    id           uuid              NOT NULL,
    user_id      uuid              NOT NULL,
    role         group_role        NOT NULL,
    status       membership_status NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_group_membership_group_user UNIQUE (group_id, user_id)
);

CREATE TABLE group_invite_links (
    is_active   boolean          NOT NULL,
    max_uses    integer,
    use_count   integer          NOT NULL,
    created_at  timestamptz      NOT NULL,
    deleted_at  timestamptz,
    expires_at  timestamptz,
    updated_at  timestamptz      NOT NULL,
    version     bigint,
    code        varchar(16)      NOT NULL,
    created_by  uuid             NOT NULL,
    group_id    uuid             NOT NULL,
    id          uuid             NOT NULL,
    type        invite_link_type NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_group_invite_link_code UNIQUE (code)
);

CREATE TABLE expenses (
    created_at         timestamptz NOT NULL,
    deleted_at         timestamptz,
    expense_date       timestamptz NOT NULL,
    total_amount_minor bigint      NOT NULL,
    updated_at         timestamptz NOT NULL,
    version            bigint,
    created_by         uuid        NOT NULL,
    currency_code      uuid        NOT NULL,
    group_id           uuid,
    id                 uuid        NOT NULL,
    split_type         split_type,
    description        varchar(255) NOT NULL,
    notes              varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE expense_payers (
    amount_paid_minor bigint      NOT NULL,
    created_at        timestamptz NOT NULL,
    updated_at        timestamptz NOT NULL,
    version           bigint,
    expense_id        uuid        NOT NULL,
    id                uuid        NOT NULL,
    user_id           uuid        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_expense_payer_expense_user UNIQUE (expense_id, user_id)
);

CREATE TABLE expense_shares (
    percentage        numeric(7,4),
    share_weight      integer,
    created_at        timestamptz NOT NULL,
    owed_amount_minor bigint      NOT NULL,
    updated_at        timestamptz NOT NULL,
    version           bigint,
    expense_id        uuid        NOT NULL,
    id                uuid        NOT NULL,
    user_id           uuid        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_expense_share_expense_user UNIQUE (expense_id, user_id)
);

CREATE TABLE settlements (
    app_initiated boolean           NOT NULL,
    amount_minor  bigint            NOT NULL,
    confirmed_at  timestamptz,
    created_at    timestamptz       NOT NULL,
    deleted_at    timestamptz,
    updated_at    timestamptz       NOT NULL,
    version       bigint,
    currency_code uuid              NOT NULL,
    from_user_id  uuid              NOT NULL,
    group_id      uuid              NOT NULL,
    id            uuid              NOT NULL,
    to_user_id    uuid              NOT NULL,
    method        settlement_method,
    status        settlement_status NOT NULL,
    note          varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE balances (
    created_at       timestamptz NOT NULL,
    net_amount_minor bigint      NOT NULL,
    updated_at       timestamptz NOT NULL,
    version          bigint,
    currency_code    uuid        NOT NULL,
    group_id         uuid        NOT NULL,
    id               uuid        NOT NULL,
    user_id          uuid        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_balance_group_user_currency UNIQUE (group_id, user_id, currency_code)
);

CREATE TABLE balance_ledger (
    created_at    timestamptz        NOT NULL,
    delta_minor   bigint             NOT NULL,
    currency_code uuid               NOT NULL,
    group_id      uuid               NOT NULL,
    id            uuid               NOT NULL,
    source_id     uuid               NOT NULL,
    user_id       uuid               NOT NULL,
    source_type   ledger_source_type NOT NULL,
    hmac          varchar(64)        NOT NULL,
    PRIMARY KEY (id)
);


-- ---------------------------------------------------------------------
-- Foreign keys
-- ---------------------------------------------------------------------
ALTER TABLE user_profiles
    ADD CONSTRAINT fk_user_profiles_user             FOREIGN KEY (id)               REFERENCES users,
    ADD CONSTRAINT fk_user_profiles_default_currency FOREIGN KEY (default_currency) REFERENCES currencies,
    ADD CONSTRAINT fk_user_profiles_language         FOREIGN KEY (language)         REFERENCES languages;

ALTER TABLE groups
    ADD CONSTRAINT fk_groups_created_by       FOREIGN KEY (created_by)       REFERENCES users,
    ADD CONSTRAINT fk_groups_default_currency FOREIGN KEY (default_currency) REFERENCES currencies;

ALTER TABLE group_memberships
    ADD CONSTRAINT fk_group_memberships_group FOREIGN KEY (group_id) REFERENCES groups,
    ADD CONSTRAINT fk_group_memberships_user  FOREIGN KEY (user_id)  REFERENCES users;

ALTER TABLE group_invite_links
    ADD CONSTRAINT fk_group_invite_links_created_by FOREIGN KEY (created_by) REFERENCES users,
    ADD CONSTRAINT fk_group_invite_links_group      FOREIGN KEY (group_id)   REFERENCES groups;

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_created_by FOREIGN KEY (created_by)    REFERENCES users,
    ADD CONSTRAINT fk_expenses_currency   FOREIGN KEY (currency_code) REFERENCES currencies,
    ADD CONSTRAINT fk_expenses_group      FOREIGN KEY (group_id)      REFERENCES groups;

ALTER TABLE expense_payers
    ADD CONSTRAINT fk_expense_payers_expense FOREIGN KEY (expense_id) REFERENCES expenses,
    ADD CONSTRAINT fk_expense_payers_user    FOREIGN KEY (user_id)    REFERENCES users;

ALTER TABLE expense_shares
    ADD CONSTRAINT fk_expense_shares_expense FOREIGN KEY (expense_id) REFERENCES expenses,
    ADD CONSTRAINT fk_expense_shares_user    FOREIGN KEY (user_id)    REFERENCES users;

ALTER TABLE settlements
    ADD CONSTRAINT fk_settlements_currency  FOREIGN KEY (currency_code) REFERENCES currencies,
    ADD CONSTRAINT fk_settlements_from_user FOREIGN KEY (from_user_id)  REFERENCES users,
    ADD CONSTRAINT fk_settlements_group     FOREIGN KEY (group_id)      REFERENCES groups,
    ADD CONSTRAINT fk_settlements_to_user   FOREIGN KEY (to_user_id)    REFERENCES users;

ALTER TABLE balances
    ADD CONSTRAINT fk_balances_currency FOREIGN KEY (currency_code) REFERENCES currencies,
    ADD CONSTRAINT fk_balances_group    FOREIGN KEY (group_id)      REFERENCES groups,
    ADD CONSTRAINT fk_balances_user     FOREIGN KEY (user_id)       REFERENCES users;

ALTER TABLE balance_ledger
    ADD CONSTRAINT fk_balance_ledger_currency FOREIGN KEY (currency_code) REFERENCES currencies,
    ADD CONSTRAINT fk_balance_ledger_group    FOREIGN KEY (group_id)      REFERENCES groups,
    ADD CONSTRAINT fk_balance_ledger_user     FOREIGN KEY (user_id)       REFERENCES users;