CREATE TABLE friendships (
  id                 VARCHAR(64) PRIMARY KEY,
  requester_user_id  VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  addressee_user_id  VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status             VARCHAR(20) NOT NULL CHECK (status IN ('pending','accepted')),
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  responded_at       TIMESTAMPTZ,
  CHECK (requester_user_id <> addressee_user_id),
  UNIQUE (requester_user_id, addressee_user_id)
);
CREATE INDEX ix_friendships_requester ON friendships(requester_user_id);
CREATE INDEX ix_friendships_addressee ON friendships(addressee_user_id);

CREATE TABLE contacts (
  id             VARCHAR(64) PRIMARY KEY,
  owner_user_id  VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name           VARCHAR(100) NOT NULL,
  avatar_url     TEXT,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_contacts_owner ON contacts(owner_user_id);

CREATE TABLE favorites (
  id             VARCHAR(64) PRIMARY KEY,
  owner_user_id  VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  target_type    VARCHAR(10) NOT NULL CHECK (target_type IN ('user','contact')),
  target_id      VARCHAR(64) NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (owner_user_id, target_type, target_id)
);
CREATE INDEX ix_favorites_owner ON favorites(owner_user_id);
