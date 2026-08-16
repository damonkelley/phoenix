# Article creation

## Command

Create an article as the active account:

```sh
realworld create-article \
  --title "Hello World" \
  --description "An introduction" \
  --body "Article contents" \
  --tag clojure \
  --tag sqlite
```

The `--tag` option is optional and may be supplied more than once.

## Authentication

Article creation requires an active account established by a successful login
using the same application database. Registration alone does not authorize
article creation.

Attempting to create an article without an active account produces:

```text
Login is required
```

Input validation occurs before the active account is resolved. An invalid
article reports its applicable validation errors rather than an authentication
error.

## Input rules

### Title

A title is required and must not be blank.

### Description

A description is required and must not be blank.

### Body

A body is required and must not be blank.

### Tags

Tags are optional non-blank strings. Supplied tags are stored as article
metadata. Tag filtering, tag listings, and other tag behavior are outside this
increment.

## Slug

The application generates a lowercase, URL-safe slug from the title. For
example:

```text
Hello World -> hello-world
```

The returned slug is the authoritative article identifier; callers must not
assume they can derive it from the title.

Slugs are globally unique. If the title-derived slug already exists, the
application appends a numeric suffix beginning with `-2` and increments it
until an unused slug is found. Duplicate article titles are therefore allowed:

```text
Hello World -> hello-world
Hello World -> hello-world-2
Hello World -> hello-world-3
```

## Success

A successful command durably stores the article, its author, and its tags in
the SQLite application database. It writes the generated slug to standard
output:

```text
hello-world
```

The slug will identify the article in subsequent listing and viewing behavior.

## Article creation errors

Known validation messages are:

```text
Title is required
Description is required
Body is required
Tag must not be blank
```

The known authentication message is:

```text
Login is required
```

Validation, authentication, or operational failure does not create an article.

## Applicable decisions

This feature follows:

- [ADR 0001: CLI command result conventions](../../adr/0001-cli-command-result-conventions.md)
- [ADR 0002: Validation error reporting](../../adr/0002-validation-error-reporting.md)
- [ADR 0003: Operational failure classification](../../adr/0003-operational-failure-classification.md)
- [ADR 0004: SQLite database location](../../adr/0004-sqlite-database-location.md)
- [ADR 0006: Active CLI account](../../adr/0006-active-cli-account.md)
