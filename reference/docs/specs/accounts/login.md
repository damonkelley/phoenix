# Account login

## Command

Log in with the email address and password of a registered account:

```sh
realworld login --email alice@example.com --password secret123
```

## Success

A successful login records the account as the active account for subsequent
commands using the same application database. Its success output is:

```text
Success
```

Registration does not log an account in automatically.

## Input rules

### Email

An email is required and follows the format rules defined by
[account registration](registration.md#email). Account lookup is
case-insensitive.

### Password

A password is required. It is not trimmed or otherwise normalized before
credential verification.

The registration password-strength rules do not apply to a login attempt. A
supplied password that does not match the stored credential is an invalid
credential regardless of its length or contents.

### Rule evaluation

Validation occurs before account lookup or password verification. An invalid
login reports only its applicable validation errors and does not change the
active account.

## Credential verification

A login succeeds only when the email identifies a registered account and the
supplied password matches that account's stored password hash.

A missing account and an incorrect password produce the same error so the
command does not disclose whether an email address is registered:

```text
Email or password is invalid
```

Credential verification must use the hashing adapter described by
[ADR 0005](../../adr/0005-password-credential-storage.md); stored hashes are not
compared with plaintext passwords.

## Session behavior

At most one account is active in an application database. A successful login
replaces any previously active account. Validation and credential failures
leave the current active account unchanged.

The active account persists across CLI processes and is scoped by the SQLite
database selected through the process's working directory. The initial slice
does not provide session expiration or a logout command.

## Login errors

Known validation messages are:

```text
Email is required
Email is invalid
Password is required
```

The known credential error is:

```text
Email or password is invalid
```

## Applicable decisions

This feature follows:

- [ADR 0001: CLI command result conventions](../../adr/0001-cli-command-result-conventions.md)
- [ADR 0002: Validation error reporting](../../adr/0002-validation-error-reporting.md)
- [ADR 0003: Operational failure classification](../../adr/0003-operational-failure-classification.md)
- [ADR 0004: SQLite database location](../../adr/0004-sqlite-database-location.md)
- [ADR 0005: Password credential storage](../../adr/0005-password-credential-storage.md)
- [ADR 0006: Active CLI account](../../adr/0006-active-cli-account.md)
