# Account registration

## Command

Register an account with an email address and password:

```sh
realworld register --email alice@example.com --password secret123
```

## Success

A successful registration creates an account with the normalized email address
and supplied password. Its success output is:

```text
Success
```

## Input rules

### Email

Leading and trailing whitespace is trimmed from an email before validation and
identity comparison. An email is required after trimming.

An email must have the form `local-part@domain` and use this practical ASCII
subset of Internet email addresses:

- The local part contains letters, digits, and `.`, `_`, `%`, `+`, or `-`.
- A dot cannot be first, last, or repeated consecutively in the local part.
- The domain contains one or more labels separated by dots.
- A domain label contains letters, digits, or hyphens and cannot begin or end
  with a hyphen.
- Whitespace is not allowed within the address.

Email validation does not verify that the domain or mailbox exists. Email
identity and uniqueness are case-insensitive.

### Password

A password is required and must contain at least 8 characters. Passwords are
not trimmed or otherwise normalized, and whitespace is not allowed.

### Rule evaluation

Validation occurs before email uniqueness is checked. An invalid registration
reports only its applicable validation errors. A failed registration does not
create an account.

## Registration errors

Known messages are:

```text
Email is required
Email is invalid
Password is required
Password must be at least 8 characters
Password must not contain whitespace
Email is already taken
```

## Applicable decisions

This feature follows:

- [ADR 0001: CLI command result conventions](../../adr/0001-cli-command-result-conventions.md)
- [ADR 0002: Validation error reporting](../../adr/0002-validation-error-reporting.md)
- [ADR 0003: Operational failure classification](../../adr/0003-operational-failure-classification.md)
- [ADR 0004: SQLite database location](../../adr/0004-sqlite-database-location.md)
