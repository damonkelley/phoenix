# 0003: Operational failure classification

## Status

Accepted

## Context

Commands can fail because user input or domain state prevents an operation, or
because infrastructure such as the database is unavailable. These failures
have different causes even when both prevent a command from completing.
Exposing exception details as command output would make implementation details
part of the public interface and could disclose sensitive information.

## Decision

Classify failures as follows:

- Validation failures describe invalid command input.
- Domain failures describe an expected business rule that prevents completion.
- Usage failures describe malformed command-line invocation.
- Operational failures describe unexpected infrastructure or runtime problems.

Operational failures produce a concise human-readable message on standard
error and do not print a stack trace by default. Their underlying cause remains
available inside the application for diagnosis but is not part of the public
command output.

Operational failures use exit status `1` as established by
[ADR 0001](0001-cli-command-result-conventions.md).

## Consequences

Feature specifications can focus on expected validation and domain behavior
without treating infrastructure exceptions as domain outcomes. The CLI shell
must translate uncaught infrastructure failures into safe diagnostics. Detailed
diagnostics will require an explicit mechanism if they are needed later.
