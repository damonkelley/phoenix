# Article feed

## Command

List the global article feed:

```sh
realworld article feed
```

The feed is public and does not require registration or login.

## Contents

The feed is a table with columns for each article's title, slug, tags, and
author email. The article body and description are omitted:

```text
TITLE        SLUG                TAGS             AUTHOR
-----------  ------------------  ---------------  -----------------
Hello World  hello-world-a1b2c3  clojure, sqlite  alice@example.com
```

Column widths expand to fit the returned values. An article without tags shows
`(none)` in the `TAGS` column. Articles are ordered from newest to oldest.
Pagination, filtering, and personalized feeds are outside this increment.

## Empty feed

Listing a database without articles succeeds and writes:

```text
No articles
```

## Failure

A database or other operational failure does not produce a partial feed and is
reported as an operational failure.

## Applicable decisions

This feature follows:

- [ADR 0001: CLI command result conventions](../../adr/0001-cli-command-result-conventions.md)
- [ADR 0003: Operational failure classification](../../adr/0003-operational-failure-classification.md)
- [ADR 0004: SQLite database location](../../adr/0004-sqlite-database-location.md)
