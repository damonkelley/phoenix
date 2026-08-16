CREATE TABLE IF NOT EXISTS articles (
  slug        TEXT PRIMARY KEY NOT NULL,
  author_id   TEXT NOT NULL REFERENCES accounts(id),
  title       TEXT NOT NULL,
  description TEXT NOT NULL,
  body        TEXT NOT NULL,
  created_at  TEXT NOT NULL,
  updated_at  TEXT NOT NULL
)
