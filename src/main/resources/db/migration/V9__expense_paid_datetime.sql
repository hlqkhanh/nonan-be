-- Issue 6: bills now carry a paid date+time (not just a date), so the UI can
-- show/sort by hour-minute. Column keeps its name (paid_date) and existing
-- date-only values become midnight timestamps.
ALTER TABLE expenses ALTER COLUMN paid_date TYPE timestamp USING paid_date::timestamp;
