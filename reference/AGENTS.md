# RealWorld reference instructions

Read `README.md` and `SPEC.md` before making changes in this directory.

## Current phase

This directory is the completed phase 1 reference application described in
`../APPROACH.md`. Its behavior and implementation are frozen at the
`reference-phase-1` tag.

Do not change application source, migrations, implementation-focused tests,
feature specifications, or ADRs. Do not add Phoenix tooling, generated code, or
phase 2 behavioral contracts here. New behavioral evidence must remain separate
from the implementation it evaluates.

Changes in this directory are limited to explicitly agreed artifact packaging
or reproducibility work. Such changes must preserve the frozen CLI behavior and
must not become a source of behavioral requirements.

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

The tests in this directory remain implementation evidence from phase 1. They
are not the independent phase 2 contract suite.

`docs/specs/` records the frozen public behavior. Phase 2 characterization may
use those specifications and observable CLI execution, but must not derive
contracts from implementation namespaces, internal tests, source structure,
migrations, or database structure.

Do not consult sibling Phoenix implementations.
