# 0001: CLI command result conventions

## Status

Accepted

## Context

The reference application exposes behavior through command-line commands. Its
feature specifications need consistent process-level conventions so each
feature does not independently decide where output is written or which exit
status represents success.

## Decision

CLI commands follow these conventions:

- User-facing success output is written to standard output.
- Validation, domain, usage, and operational errors are written to standard
  error.
- Successful commands exit with status `0`.
- Commands that run but cannot complete exit with status `1`.
- Malformed command-line usage exits with status `2`.
- Command-line syntax and malformed-usage reporting are handled consistently by
  the shared CLI harness rather than by individual features.

Feature specifications remain responsible for defining their successful output
and expected domain and validation messages.

## Consequences

Black-box tests can distinguish successful output from diagnostics and can use
exit status without knowing application internals. Feature specifications may
reference this decision instead of repeating the process-level conventions.

Operational, domain, and validation failures share exit status `1`; callers
that need to distinguish them must inspect their error output.
