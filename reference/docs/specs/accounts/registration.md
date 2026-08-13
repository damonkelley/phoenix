# Account registration

## Command

Register an account with an email address and password:

```sh
realworld register --email alice@example.com --password secret123
```

## Success

A successful registration creates an account with the supplied email address
and password. Its success output is:

```text
Success
```

## Input rules

Leading and trailing whitespace is trimmed from input before validation.

- Email is required and must be a standard email address.
- Email uniqueness is case-insensitive.
- Password is required and must contain at least 8 characters.

## Registration errors

Known messages are:

```text
Email is required
Email is invalid
Password is required
Password must be at least 8 characters
Email is already taken
```

## Applicable decisions

This feature follows:

- [ADR 0001: CLI command result conventions](../../adr/0001-cli-command-result-conventions.md)
- [ADR 0002: Validation error reporting](../../adr/0002-validation-error-reporting.md)
- [ADR 0003: Operational failure classification](../../adr/0003-operational-failure-classification.md)
- [ADR 0004: SQLite database location](../../adr/0004-sqlite-database-location.md)
