-- Phase 2: groups become "quick-select bundles" of participants (users/contacts),
-- owned by a user. They no longer own a ledger. The legacy per-group `members`
-- table is left untouched here (still used by ledger/expense/settlement until
-- the Phase 3 migration, V5, re-scopes those to owner_user_id).

CREATE TABLE group_members (
  id          VARCHAR(64) PRIMARY KEY,
  group_id    VARCHAR(64) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  target_type VARCHAR(10) NOT NULL CHECK (target_type IN ('user','contact')),
  target_id   VARCHAR(64) NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (group_id, target_type, target_id)
);
CREATE INDEX ix_group_members_group ON group_members(group_id);
