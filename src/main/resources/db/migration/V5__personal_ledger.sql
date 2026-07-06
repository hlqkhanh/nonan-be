-- Phase 3: bills & ledgers become personal per-user (owner_user_id), no longer
-- group-owned. Participant identity is now a prefixed string id
-- (user:<userId> / contact:<contactId>) held in expense_payers.member_id,
-- expense_participants.member_id, and settlement_snapshots/adjustments/
-- paid_settlements from/to member_id + pair_key columns. Those columns keep
-- their names (still plain strings) — only their FK constraints and meaning
-- change.
--
-- DATA-MIGRATION DECISION (this is a dev/seed database, confirmed acceptable
-- per project instructions): re-mapping every legacy per-group member_id to a
-- prefixed user:/contact: participant id would require synthesizing a real
-- `contact` row for every anonymous "ghost" member (a member row with no
-- user_id and no corresponding contact), then rewriting every expense_payers/
-- expense_participants/settlement_* row plus the ledger_cycles/expenses
-- ownership itself (which user's personal ledger does a shared group's
-- history even belong to?). That's a lot of one-off backfill logic to support
-- a handful of local dev/seed rows. We instead TRUNCATE the bill/ledger data
-- below and start the new personal-ledger model clean. We do NOT touch
-- users / friendships / contacts / favorites / bill_title_templates / groups
-- / group_members.

TRUNCATE TABLE audit_logs, settlement_adjustments, paid_settlements, settlement_snapshots,
  expense_payers, expense_participants, expenses, ledger_cycles RESTART IDENTITY CASCADE;

-- Drop FKs to the old per-group members table; these columns become plain
-- prefixed participant ids (user:<id> / contact:<id>) with no FK.
ALTER TABLE expense_payers DROP CONSTRAINT IF EXISTS expense_payers_member_id_fkey;
ALTER TABLE expense_participants DROP CONSTRAINT IF EXISTS expense_participants_member_id_fkey;
ALTER TABLE ledger_cycles DROP CONSTRAINT IF EXISTS ledger_cycles_closed_by_member_id_fkey;

-- ledger_cycles: owner-scoped (one open cycle per user), not group-scoped.
ALTER TABLE ledger_cycles ALTER COLUMN group_id DROP NOT NULL;
ALTER TABLE ledger_cycles ADD COLUMN owner_user_id VARCHAR(64) NOT NULL REFERENCES users(id);
ALTER TABLE ledger_cycles RENAME COLUMN closed_by_member_id TO closed_by_user_id;

DROP INDEX IF EXISTS ux_ledger_cycles_one_open;
CREATE UNIQUE INDEX ux_ledger_cycles_one_open ON ledger_cycles(owner_user_id) WHERE status = 'open';
CREATE INDEX ix_ledger_cycles_owner ON ledger_cycles(owner_user_id);

-- expenses: owner-scoped, not group-scoped. group_id kept as a nullable,
-- unmapped legacy column (no longer used by the application).
ALTER TABLE expenses ALTER COLUMN group_id DROP NOT NULL;
ALTER TABLE expenses ADD COLUMN owner_user_id VARCHAR(64) NOT NULL REFERENCES users(id);
CREATE INDEX ix_expenses_owner ON expenses(owner_user_id);

-- audit_logs: no more per-group scoping/actor-within-group; the actor is
-- always the ledger's owner (personal ledgers have exactly one user).
ALTER TABLE audit_logs DROP COLUMN group_id;
ALTER TABLE audit_logs DROP COLUMN actor_member_id;
ALTER TABLE audit_logs ADD COLUMN owner_user_id VARCHAR(64) NOT NULL REFERENCES users(id);
CREATE INDEX ix_audit_owner ON audit_logs(owner_user_id);

-- The per-group members table is fully superseded by group_members (bundles)
-- plus prefixed participant ids on expenses/settlements.
DROP TABLE members;
