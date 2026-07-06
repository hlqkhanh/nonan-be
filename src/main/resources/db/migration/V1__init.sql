CREATE TABLE users (
  id            VARCHAR(64) PRIMARY KEY,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name  VARCHAR(100) NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE groups (
  id                 VARCHAR(64) PRIMARY KEY,
  name               VARCHAR(150) NOT NULL,
  created_by_user_id VARCHAR(64) REFERENCES users(id),
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE members (
  id         VARCHAR(64) PRIMARY KEY,
  group_id   VARCHAR(64) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  name       VARCHAR(100) NOT NULL,
  user_id    VARCHAR(64) REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (group_id, user_id)
);
CREATE INDEX ix_members_group ON members(group_id);
CREATE INDEX ix_members_user  ON members(user_id);

CREATE TABLE ledger_cycles (
  id                  VARCHAR(64) PRIMARY KEY,
  group_id            VARCHAR(64) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  status              VARCHAR(20) NOT NULL CHECK (status IN ('open','settled','archived_unpaid')),
  start_date          DATE NOT NULL,
  end_date            DATE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  closed_at           TIMESTAMPTZ,
  closed_by_member_id VARCHAR(64) REFERENCES members(id)
);
CREATE UNIQUE INDEX ux_ledger_cycles_one_open ON ledger_cycles(group_id) WHERE status = 'open';
CREATE INDEX ix_ledger_cycles_group ON ledger_cycles(group_id);

CREATE TABLE expenses (
  id              VARCHAR(64) PRIMARY KEY,
  group_id        VARCHAR(64) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  ledger_cycle_id VARCHAR(64) NOT NULL REFERENCES ledger_cycles(id),
  title           VARCHAR(200) NOT NULL,
  total_amount    BIGINT NOT NULL CHECK (total_amount >= 0),
  paid_date       DATE NOT NULL,
  image_url       TEXT,
  split_mode      VARCHAR(10) NOT NULL CHECK (split_mode IN ('equal','custom')),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_expenses_cycle ON expenses(ledger_cycle_id);
CREATE INDEX ix_expenses_group ON expenses(group_id);

CREATE TABLE expense_payers (
  id         BIGSERIAL PRIMARY KEY,
  expense_id VARCHAR(64) NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
  member_id  VARCHAR(64) NOT NULL REFERENCES members(id),
  amount     BIGINT NOT NULL,
  position   INT NOT NULL DEFAULT 0
);
CREATE INDEX ix_expense_payers_expense ON expense_payers(expense_id);

CREATE TABLE expense_participants (
  id          BIGSERIAL PRIMARY KEY,
  expense_id  VARCHAR(64) NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
  member_id   VARCHAR(64) NOT NULL REFERENCES members(id),
  amount      BIGINT NOT NULL,
  is_custom   BOOLEAN NOT NULL DEFAULT FALSE,
  member_name VARCHAR(100),
  position    INT NOT NULL DEFAULT 0
);
CREATE INDEX ix_expense_participants_expense ON expense_participants(expense_id);

CREATE TABLE settlement_snapshots (
  id              VARCHAR(64) PRIMARY KEY,
  ledger_cycle_id VARCHAR(64) NOT NULL REFERENCES ledger_cycles(id) ON DELETE CASCADE,
  from_member_id  VARCHAR(64) NOT NULL,
  to_member_id    VARCHAR(64) NOT NULL,
  amount          BIGINT NOT NULL,
  paid            BOOLEAN NOT NULL
);
CREATE INDEX ix_snapshots_cycle ON settlement_snapshots(ledger_cycle_id);

CREATE TABLE settlement_adjustments (
  id              BIGSERIAL PRIMARY KEY,
  ledger_cycle_id VARCHAR(64) NOT NULL REFERENCES ledger_cycles(id) ON DELETE CASCADE,
  pair_key        VARCHAR(140) NOT NULL,
  delta           BIGINT NOT NULL,
  UNIQUE (ledger_cycle_id, pair_key)
);

CREATE TABLE paid_settlements (
  id              BIGSERIAL PRIMARY KEY,
  ledger_cycle_id VARCHAR(64) NOT NULL REFERENCES ledger_cycles(id) ON DELETE CASCADE,
  pair_key        VARCHAR(140) NOT NULL,
  UNIQUE (ledger_cycle_id, pair_key)
);

CREATE TABLE audit_logs (
  id              VARCHAR(64) PRIMARY KEY,
  group_id        VARCHAR(64) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  ledger_cycle_id VARCHAR(64),
  actor_member_id VARCHAR(64) NOT NULL,
  action          VARCHAR(40) NOT NULL,
  entity_type     VARCHAR(20) NOT NULL,
  entity_id       VARCHAR(64) NOT NULL,
  summary         TEXT NOT NULL,
  before_json     JSONB,
  after_json      JSONB,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_cycle ON audit_logs(ledger_cycle_id);
CREATE INDEX ix_audit_group ON audit_logs(group_id);
