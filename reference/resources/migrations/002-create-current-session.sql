CREATE TABLE IF NOT EXISTS current_session (
  singleton  INTEGER PRIMARY KEY NOT NULL CHECK (singleton = 1),
  account_id TEXT NOT NULL REFERENCES accounts(id)
)
