# Article view

## Command

View an article by its slug:

```sh
realworld article view --slug hello-world-a1b2c3
```

Viewing an article is public and does not require registration or login.

## Success

A successful command writes the article's title, slug, description, tags,
author email, and body as labeled plain text:

```text
Title: Hello World
Slug: hello-world-a1b2c3
Description: An introduction
Tags: clojure, sqlite
Author: alice@example.com

Body:
Article contents
```

An article without tags is written with:

```text
Tags: (none)
```

The article body is written without modification and may span multiple lines.
Timestamps and other article metadata are omitted.

## Input rules

A slug is required and must not be blank. Missing or blank input produces:

```text
Slug is required
```

Input validation occurs before article lookup.

## Missing article

A valid slug that does not identify an article in the application database
produces:

```text
Article not found
```

## Applicable decisions

This feature follows:

- [ADR 0001: CLI command result conventions](../../adr/0001-cli-command-result-conventions.md)
- [ADR 0002: Validation error reporting](../../adr/0002-validation-error-reporting.md)
- [ADR 0003: Operational failure classification](../../adr/0003-operational-failure-classification.md)
- [ADR 0004: SQLite database location](../../adr/0004-sqlite-database-location.md)
