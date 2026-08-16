# RealWorld reference instructions

Read `README.md` and `SPEC.md` before making changes in this directory. Before
changing application behavior, also read the applicable feature specifications
under `docs/specs/` and the ADRs they reference. If no applicable feature
specification exists, agree on the intended behavior with the user and add one
before implementing it.

## Current phase

This directory is phase 1 of the experiment described in `../APPROACH.md`.
Build only the conventional RealWorld reference application. Do not introduce
Phoenix tooling, generated code, specification machinery, or a proposed Phoenix
directory structure.

The initial milestone is a standalone JVM Clojure CLI application that can
register a user, log in, create an article, list the global feed, view an
article, and persist users and articles in SQLite. A web interface comes later.

Build incrementally. Discuss the next vertical behavior with the user rather
than implementing the entire milestone speculatively.

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

Prefer tests at public boundaries. Behavior eventually used for regeneration
must not depend on reference implementation namespaces or internal data
structures. Keep implementation-focused unit tests separate from black-box CLI
contracts.

Treat `docs/specs/` as the repository's current public behavior and keep it in
sync with intentional behavior changes. Specifications should describe
observable outcomes and rules rather than implementation structure.

Use the public RealWorld documentation to clarify product behavior. Do not
consult sibling Phoenix implementations.
