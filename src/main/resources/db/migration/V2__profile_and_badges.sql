ALTER TABLE users ADD COLUMN username VARCHAR(50);
ALTER TABLE users ADD COLUMN avatar_url TEXT;

WITH base AS (
  SELECT id,
         NULLIF(regexp_replace(lower(split_part(email, '@', 1)), '[^a-z0-9]+', '', 'g'), '') AS slug
  FROM users
),
numbered AS (
  SELECT id,
         COALESCE(slug, 'user') AS slug,
         row_number() OVER (PARTITION BY COALESCE(slug, 'user') ORDER BY id) AS rn
  FROM base
)
UPDATE users u
SET username = CASE WHEN n.rn = 1 THEN n.slug ELSE n.slug || n.rn::text END
FROM numbered n
WHERE u.id = n.id;

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT ux_users_username UNIQUE (username);

CREATE TABLE bill_title_templates (
  id            VARCHAR(64) PRIMARY KEY,
  owner_user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  label         VARCHAR(40) NOT NULL,
  position      INT NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_bill_title_templates_owner ON bill_title_templates(owner_user_id);
