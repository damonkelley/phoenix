# 0006: Persistent CLI session

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

A successful CLI login starts a persistent session for the authenticated
account in the SQLite application database. At most one CLI session exists in
a database at a time, and another successful login replaces it.

Subsequent CLI commands that require authentication resolve the authenticated
account represented by the current session. Registration does not start a
session. Failed login attempts do not clear or replace the current session.

The initial CLI does not expose authentication tokens, session expiration, or
logout behavior. A future web interface may represent authentication
differently while reusing the application credential-verification behavior.

## Consequences

Login state persists across one-shot CLI processes and follows the database
location conventions in [ADR 0004](0004-sqlite-database-location.md). Commands
run from different working directories have independent sessions.

SQLite persistence must represent the current session and preserve referential
integrity with stored accounts. Black-box login scenarios can use temporary
working directories to isolate both accounts and session state.

This local session representation is not the authentication contract for a
future HTTP interface. Adding logout, session expiration, multiple concurrent
sessions, or token issuance requires further behavior and decisions.
