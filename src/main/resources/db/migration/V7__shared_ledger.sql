-- Soft delete bills
ALTER TABLE expenses ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE expenses ADD COLUMN deleted_by_user_id VARCHAR(64) REFERENCES users(id);
CREATE INDEX ix_expenses_cycle_active ON expenses(ledger_cycle_id) WHERE deleted_at IS NULL;

-- Thanh vien khoan no (chia se) + trang thai pin per-user. Mirror group_members (V6).
CREATE TABLE ledger_cycle_members (
  ledger_cycle_id VARCHAR(64) NOT NULL REFERENCES ledger_cycles(id) ON DELETE CASCADE,
  user_id         VARCHAR(64) NOT NULL REFERENCES users(id),
  pinned          BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (ledger_cycle_id, user_id)
);
CREATE INDEX ix_ledger_cycle_members_user ON ledger_cycle_members(user_id);

-- Backfill: owner luon la thanh vien (pinned) + moi user: participant/payer san co.
INSERT INTO ledger_cycle_members (ledger_cycle_id, user_id, pinned)
  SELECT id, owner_user_id, TRUE FROM ledger_cycles
ON CONFLICT DO NOTHING;
INSERT INTO ledger_cycle_members (ledger_cycle_id, user_id, pinned)
  SELECT DISTINCT e.ledger_cycle_id, substring(p.member_id FROM 6), TRUE
  FROM expense_participants p JOIN expenses e ON e.id = p.expense_id
  WHERE p.member_id LIKE 'user:%'
ON CONFLICT DO NOTHING;
INSERT INTO ledger_cycle_members (ledger_cycle_id, user_id, pinned)
  SELECT DISTINCT e.ledger_cycle_id, substring(py.member_id FROM 6), TRUE
  FROM expense_payers py JOIN expenses e ON e.id = py.expense_id
  WHERE py.member_id LIKE 'user:%'
ON CONFLICT DO NOTHING;
