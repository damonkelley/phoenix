# 0006: Active CLI account

## Status

Accepted

## Context

The application runs each CLI command in a separate process. After login,
commands such as article creation need to identify the authenticated account
without keeping the login process alive.

The full RealWorld HTTP API uses tokens, but requiring users to copy a token
between commands is not necessary for this local CLI. The reference still
needs explicit, durable authentication behavior rather than treating a
successful password check as a session that disappears when the process exits.

## Decision

A successful CLI login records the authenticated account as the active account
in the SQLite application database. At most one account is active in a database
at a time, and another successful login replaces it.

Subsequent CLI commands that require authentication use the active account.
Registration does not activate an account. Failed login attempts do not clear
or replace an existing active account.

The initial CLI does not expose authentication tokens, session expiration, or
logout behavior. A future web interface may represent authentication
differently while reusing the application credential-verification behavior.

## Consequences

Login state persists across one-shot CLI processes and follows the database
location conventions in [ADR 0004](0004-sqlite-database-location.md). Commands
run from different working directories have independent active accounts.

SQLite persistence must represent the active account and preserve referential
integrity with stored accounts. Black-box login scenarios can use temporary
working directories to isolate both accounts and session state.

This local session representation is not the authentication contract for a
future HTTP interface. Adding logout, session expiration, multiple concurrent
sessions, or token issuance requires further behavior and decisions.
