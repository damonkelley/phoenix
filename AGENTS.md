# Project instructions

Read `README.md`, `reference/README.md`, and `reference/SPEC.md` before making changes.

## Current phase

Work only on the conventional RealWorld reference application under
`reference/`. Do not introduce Phoenix tooling, generated code, specification
machinery, or a proposed Phoenix directory structure unless the user explicitly
changes the phase.

The initial reference milestone is a standalone JVM Clojure CLI application
that can register a user, log in, create an article, list the global feed, view
an article, and persist users and articles in SQLite. A web interface comes
later.

Build this incrementally. Discuss the next vertical behavior with the user
rather than implementing the entire milestone speculatively.

## Architecture

- Keep the application layer independent of CLI and future web interfaces.
- Model the functional core as decisions over a command and resolved coeffects,
  returning events and requested effects as data.
- Keep coeffect resolution and effect interpretation in the imperative shell.
- Use interceptor composition initially for validation, coeffect resolution,
  domain decision, and effect interpretation.
- Treat interceptors as an implementation mechanism, not as a future Markdown
  authoring language.
- Keep implementation choices conventional and understandable; do not optimize
  the reference for later code generation.

## Behavioral evidence

Prefer tests at public boundaries. The behavior eventually used for
regeneration must not depend on reference implementation namespaces or internal
data structures. Keep implementation-focused unit tests separate from black-box
CLI contracts.

## Isolation

Do not inspect, read, search, copy, or derive implementation ideas from sibling
repositories. They are deliberately excluded from this agent's context. Work
only with files inside this repository and the public RealWorld documentation
unless the user explicitly requests otherwise.
