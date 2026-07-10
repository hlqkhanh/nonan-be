-- Issue 1-3: ledger_cycles.status collapses to 2 values (archived_unpaid,
-- settled) — 'open' is retired. "Open" becomes a per-viewer flag instead:
-- ledger_cycle_members.pinned is renamed to `active`, and the application
-- guarantees exactly one active=true row per user (their home-screen cycle).

-- 1) Drop the "one open cycle per owner" constraint (status 'open' is retired).
DROP INDEX IF EXISTS ux_ledger_cycles_one_open;

-- 2) Rename 'pinned' -> 'active' (default false), then backfill exactly one
--    active row per user: the cycle they owned that was 'open'.
ALTER TABLE ledger_cycle_members RENAME COLUMN pinned TO active;
ALTER TABLE ledger_cycle_members ALTER COLUMN active SET DEFAULT FALSE;
UPDATE ledger_cycle_members SET active = FALSE;
UPDATE ledger_cycle_members m SET active = TRUE
  FROM ledger_cycles c
  WHERE c.id = m.ledger_cycle_id AND c.owner_user_id = m.user_id AND c.status = 'open';

-- 3) Merge status: open -> archived_unpaid (open and "chua tra" are now the
--    same underlying state; "open" only meant "currently the home screen
--    cycle", which is now represented by the per-viewer `active` flag above).
UPDATE ledger_cycles SET status = 'archived_unpaid' WHERE status = 'open';

-- 4) Tighten the status check constraint to the 2 remaining values.
ALTER TABLE ledger_cycles DROP CONSTRAINT IF EXISTS ledger_cycles_status_check;
ALTER TABLE ledger_cycles ADD CONSTRAINT ledger_cycles_status_check CHECK (status IN ('archived_unpaid', 'settled'));
