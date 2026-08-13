# 0002: Validation error reporting

## Status

Accepted

## Context

A command can contain several invalid values. Stopping at the first error makes
users repeatedly submit a command to discover independent problems, while
reporting every mechanically triggered rule can produce redundant messages.
Feature specifications need a shared validation policy without prescribing
implementation structure.

## Decision

Command validation follows these conventions:

- Report all applicable validation errors found in one evaluation.
- Write each error as a separate line.
- Do not make the order of validation errors part of the public contract.
- Do not report dependent constraints when a required value is absent. For
  example, an absent password reports its required-value error but not its
  minimum-length error.
- Feature specifications define their validation rules and exact error
  messages.

Validation errors use the error stream and exit status established by
[ADR 0001](0001-cli-command-result-conventions.md).

## Consequences

Users can correct independent input problems in one pass. Black-box tests may
compare the set of emitted validation lines but must not depend on their order.
Validators need to distinguish an absent value from a present value that fails
additional constraints.
