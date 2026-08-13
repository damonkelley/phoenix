# 0004: SQLite database location

## Status

Accepted

## Context

The reference application needs durable local persistence without requiring a
separate database service. CLI commands also need a predictable way to locate
the same application state, while black-box tests need isolated state that is
easy to create and discard.

## Decision

The reference application uses SQLite for durable persistence.

By default, its database is the file `realworld.db` in the process's current
working directory. Commands that use persistent state resolve the same default
location.

A configurable database location is not part of the current behavior. It can
be introduced later if concrete usage requires it.

Database access and storage failures are operational failures under
[ADR 0003](0003-operational-failure-classification.md).

## Consequences

The application remains self-contained and requires no database service. A user
controls which application state is used by choosing the working directory.
Running commands from a temporary directory gives black-box tests an isolated
database that can be discarded after each scenario.

Commands run from different working directories use different databases. The
application must manage SQLite schema creation or migration before accessing
stored data.
