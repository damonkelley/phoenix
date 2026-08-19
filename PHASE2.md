# Phase 2 handoff: freeze and characterize behavior

## Starting point

Phase 1 is complete. The reference application's behavior and implementation
are frozen at the annotated Git tag `reference-phase-1`, which points to commit
`d8be109`. Commit `b944729` moved the repository into phase 2 and established
the isolation rules for this work.

The frozen reference is a standalone JVM Clojure CLI implementing the agreed
RealWorld slice: account registration and login, authenticated article
creation, the public global feed, article viewing, and SQLite persistence.

## Purpose

Phase 2 must produce durable evidence that can distinguish a behaviorally
correct replacement from an incorrect one without consulting the reference
implementation.

This is the repository's deletion test. The phase is not complete merely
because the existing implementation tests pass. It is complete when an opaque,
reproducible reference artifact can be evaluated repeatedly at its public
boundary and the evaluations detect meaningful behavioral mutations.

## Evidence boundary

A behavior-characterization agent may use:

- `README.md`, `APPROACH.md`, and this handoff;
- the public feature documentation under `reference/docs/specs/`;
- the ADRs linked directly from those feature specifications;
- the public RealWorld documentation when clarification is necessary;
- observable execution of the packaged reference artifact.

It must not inspect or derive behavioral requirements from:

- `reference/src/` or `reference/test/`;
- database migrations, the SQLite database, or internal data structures;
- source history, diffs, or commits that reveal implementation details;
- sibling repositories.

`reference/SPEC.md` is the historical phase 1 implementation handoff. It is not
a proposed Phoenix specification format or an additional source of phase 2
requirements.

## Next work

### 1. Package the reference artifact

Perform artifact packaging as a separately scoped task. That task may inspect
the reference project only as needed to build a reproducible executable. It
must:

- preserve the behavior frozen at `reference-phase-1`;
- document the exact source revision, toolchain, build command, and invocation;
- avoid introducing application behavior or Phoenix machinery;
- leave contract authoring to a later fresh session.

The artifact format and whether build outputs are committed remain open. Agree
on those choices before adding build machinery.

### 2. Characterize behavior in a fresh session

After the artifact exists, begin contract work with a fresh agent context that
has not inspected reference internals. Before adding a new top-level project,
agree on its name, runtime, and invocation protocol.

The contract runner should:

- accept a configured executable rather than reference-specific namespaces;
- run each scenario in an isolated temporary workspace starting from a clean
  database;
- observe only command arguments, exit status, standard output, standard error,
  and persistence visible through later public commands;
- tolerate intentional nondeterminism through properties rather than captured
  values;
- run unchanged against future replacement implementations.

Begin by inventorying observations and classifying each as a durable product
obligation, an operational constraint, incidental reference behavior, or an
unresolved decision. Do not automatically conserve every observable quirk.
Account registration is the recommended first regenerative grain, subject to
confirmation before implementation.

## Phase exit criteria

Phase 2 ends when:

- a reproducible reference artifact can be built and invoked without contract
  code depending on implementation details;
- black-box contracts cover the agreed CLI slice from clean state and across
  process boundaries;
- the contracts run repeatedly and deterministically apart from explicitly
  modeled nondeterminism;
- meaningful mutations demonstrate that the contracts reject materially broken
  behavior;
- public contracts, any hidden evaluation evidence, and implementation-focused
  tests have clearly separated roles;
- no architecture generalization, generator design, or Phoenix runtime has been
  smuggled into the characterization work.

## Rationale

This phase follows the principles articulated in Chad Fowler's Phoenix
Architecture series: preserve what survives deletion, relocate rigor into
durable evaluations, recover important knowledge before discarding an
implementation, and discover a safe regenerative grain before building general
machinery.

Relevant articles include [The Deletion Test](https://aicoding.leaflet.pub/3md5ftetaes2e),
[Evaluations Are the Real Codebase](https://aicoding.leaflet.pub/3mb526js42k26),
[The Implementation Remembers](https://aicoding.leaflet.pub/3mobohx4fq22x),
[The Regenerative Grain](https://aicoding.leaflet.pub/3mfai4nqg6224), and
[The Phoenix Primitives](https://aicoding.leaflet.pub/3mjfruwwuck2d).
