# Repository instructions

Read `README.md`, `APPROACH.md`, and the current [`PHASE2.md`](PHASE2.md)
handoff before making changes. Also follow any more specific `AGENTS.md` found
in the directory being changed.

## Purpose

This repository explores regenerative software development by first
establishing real application behavior and later discovering what Phoenix
machinery is required. Do not assume that the current directory structure
represents a final Phoenix product architecture.

## Current phase

Phase 1 is complete. The reference application's behavior is frozen at the
`reference-phase-1` tag. The repository is now in phase 2: freeze and
characterize that behavior by establishing the public CLI behavioral oracle
required for a later deletion test.

During this phase:

- do not change the reference application's behavior, source, migrations,
  feature specifications, ADRs, or implementation-focused tests;
- keep new behavioral evidence separate from `reference/`;
- do not begin architecture generalization, implementation generation, or
  Phoenix tooling;
- limit changes under `reference/` to explicitly agreed, decision-gated
  reproducibility or packaging work that preserves the frozen behavior.

## Working approach

- Work incrementally and discuss consequential design choices with the user.
- Do not introduce speculative top-level structure, frameworks, or platform
  concerns.
- Keep independently runnable projects conventional and self-contained.
- Preserve public behavioral evidence separately from implementation details
  when that distinction becomes relevant.
- Treat recorded direction as a hypothesis to test, not an immutable roadmap.

## Isolation

Behavior-characterization work must begin in a fresh agent session and treat the
reference implementation as opaque. It may use the public feature documentation
under `reference/docs/specs/`, the decisions linked from those specifications,
and observable execution of the opaque reference command. It must not inspect
or derive contracts from `reference/src/`, `reference/test/`, migrations, the
SQLite database, or source history and diffs that expose implementation
details.

A separately scoped reproducibility or packaging task, if agreed, may inspect
the reference only as needed to make the opaque command reproducibly invocable,
but implementation observations from that task must not become behavioral
contracts.

Do not inspect, read, search, copy, or derive implementation ideas from sibling
repositories unless the user explicitly requests it. Sibling projects are
deliberately excluded from this repository's agent context.
