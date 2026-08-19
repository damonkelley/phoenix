# Repository instructions

Read `README.md` and `APPROACH.md` before making changes. Also follow any more
specific `AGENTS.md` found in the directory being changed.

## Purpose

This repository explores regenerative software development by first
establishing real application behavior and later discovering what Phoenix
machinery is required. Do not assume that the current directory structure
represents a final Phoenix product architecture.

## Current phase

Phase 1 is complete. The reference application's behavior is frozen at the
`reference-phase-1` tag. The repository is now in phase 2: freeze and
characterize that behavior through contracts at the public CLI boundary.

During this phase:

- do not change the reference application's behavior, source, migrations,
  feature specifications, ADRs, or implementation-focused tests;
- keep new behavioral evidence separate from `reference/`;
- do not begin architecture generalization, implementation generation, or
  Phoenix tooling;
- limit changes under `reference/` to explicitly agreed packaging or
  reproducibility work that preserves the frozen behavior.

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
and an executable reference artifact. It must not inspect or derive contracts
from `reference/src/`, `reference/test/`, migrations, the SQLite database, or
source history and diffs that expose implementation details.

A separately scoped artifact-packaging task may inspect the reference as needed
to produce the executable, but implementation observations from that task must
not become behavioral contracts.

Do not inspect, read, search, copy, or derive implementation ideas from sibling
repositories unless the user explicitly requests it. Sibling projects are
deliberately excluded from this repository's agent context.
