CREATE TABLE IF NOT EXISTS article_tags (
  article_slug TEXT NOT NULL REFERENCES articles(slug) ON DELETE CASCADE,
  position     INTEGER NOT NULL,
  tag          TEXT NOT NULL,
  PRIMARY KEY (article_slug, position)
)
