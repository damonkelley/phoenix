# Phase 2 handoff: freeze and characterize behavior

## Starting point

Phase 1 is complete. The reference application's behavior and implementation
are frozen at the annotated Git tag `reference-phase-1`, which points to commit
`d8be109`. Commit `b944729` moved the repository into phase 2 and established
the isolation rules for this work.

The frozen reference is a standalone JVM Clojure CLI implementing the agreed
RealWorld slice: account registration and login, authenticated article
creation, the public global feed, article viewing, and SQLite persistence.

A phase 2 reproducibility-only launcher change now makes the opaque command
invocable from arbitrary working directories without changing the caller's
working directory. It uses the exact Java, Clojure CLI, and Babashka versions
already pinned in `reference/.mise.toml`; it does not introduce a packaged
artifact or change frozen application source.

## Purpose

Phase 2 must establish a durable public CLI behavioral oracle that can
distinguish a materially correct black-box candidate from an incorrect one
without consulting the reference implementation.

This oracle is required for a later deletion test; phase 2 is not the complete
deletion test. It neither deletes and regenerates an implementation nor
captures every kind of evidence that safe regeneration will require. The phase
is not complete merely because the existing implementation tests pass. It is
complete when an opaque reference command is reproducibly invocable and
repeatable public-boundary evaluations reject materially faulty black-box
candidates.

## Evidence boundary

A behavior-characterization agent may use:

- `README.md`, `APPROACH.md`, and this handoff;
- the public feature documentation under `reference/docs/specs/`;
- the ADRs linked directly from those feature specifications;
- the public RealWorld documentation when clarification is necessary;
- observable execution of the opaque reference command.

It must not inspect or derive behavioral requirements from:

- `reference/src/` or `reference/test/`;
- database migrations, the SQLite database, or internal data structures;
- source history, diffs, or commits that reveal implementation details;
- sibling repositories.

`reference/SPEC.md` is the historical phase 1 implementation handoff. It is not
a proposed Phoenix specification format or an additional source of phase 2
requirements.

The CLI evidence boundary is intentionally incomplete. Durable obligations
that are not observable there, such as accepted requirements about credential
storage, must be recorded for later evidence mechanisms rather than inferred
from CLI behavior or silently dropped.

## Next work

### 1. Reproducible opaque invocation established

The least enabling increment uses the existing project-local mise toolchain and
launcher rather than producing an uberjar, native image, container, or committed
binary. The application source remains frozen at the annotated tag
`reference-phase-1` (`d8be109886efe75ee663548a59af218ff4986f5b`). Provision it
from the repository root with:

```sh
mise install -C reference java clojure babashka
```

The opaque command is `reference/bin/realworld`. It locates the project and its
pinned Java and Clojure executables independently of the caller's location while
leaving the caller's working directory unchanged. There is no build command.
Exact test, formatting, and temporary-workspace invocation commands are recorded
in `reference/README.md`.

### 2. Behavior characterization in progress

The independently runnable [`cli-contracts/`](cli-contracts/) project now holds
reviewed obligation ledgers, separate non-normative black-box observations, and
the contract runner. It uses an independently pinned Babashka runtime and
accepts exactly one configured target executable without a reference-specific
default. Each new feature slice must still begin in a fresh agent context that
has not inspected reference internals.

The contract runner must continue to:

- accept a configured executable rather than reference-specific namespaces;
- run each scenario in an isolated temporary workspace starting from clean
  state;
- observe only command arguments, exit status, standard output, standard error,
  and persistence visible through later public commands;
- tolerate intentional nondeterminism through properties rather than captured
  values;
- run unchanged against future replacement implementations.

Current equivalence is scoped to clean state, including persistence observable
across separate command processes within a scenario. It does not prove that a
replacement can inherit existing reference data. Existing-data continuity
becomes an obligation only if it is chosen and supported by later evidence.

Before any observed behavior is promoted into a contract, create and review an
obligation ledger. Classify each candidate as a durable product obligation, an
operational constraint, incidental reference behavior, or an unresolved
decision, and record its rationale and provenance from permitted documentation
or black-box observation. Observability alone is not a reason to conserve a
quirk. The ledger should also identify accepted durable obligations that the
CLI cannot establish so they can receive later evidence mechanisms.

Account registration is the completed first characterization slice. Its six
reviewed scenarios run repeatedly against the opaque reference command, and
three deliberately faulty command substitutes demonstrate discrimination of
invalid acceptance, incomplete validation reporting, and case-sensitive
identity. This is not yet a regenerative grain; that requires later evidence
about replacement boundaries, mutation ownership, and deletion safety. Stop
and review this increment before expanding to another feature.

Use meaningful mutations to test the oracle's discrimination. Here that means
deliberately faulty black-box candidates or command substitutes whose
observable behavior violates reviewed obligations. Do not mutate or inspect the
frozen reference to create them.

Whether evaluations are public, hidden, or a mixture remains an open decision.
Hidden evaluation machinery is not required without an articulated need or
threat model.

## Phase exit criteria

Phase 2 ends when:

- the opaque reference command can be reproduced and invoked without contract
  code depending on implementation details;
- the obligation ledger has been reviewed before observations are promoted;
- black-box contracts cover the agreed CLI slice from clean state and across
  process boundaries, without claiming existing-data continuity;
- the contracts run repeatedly and deterministically apart from explicitly
  modeled nondeterminism;
- agreed faulty black-box candidates demonstrate that the contracts reject
  materially broken behavior without mutating the frozen reference;
- behavioral contracts and implementation-focused tests have clearly separated
  roles;
- durable obligations outside CLI observability remain identified for later
  evidence rather than being treated as proven;
- no architecture generalization, generator design, or Phoenix runtime has been
  smuggled into the characterization work.

## Rationale

This phase follows the principles articulated in Chad Fowler's Phoenix
Architecture series: preserve what survives deletion, relocate rigor into
durable evaluations, recover important knowledge before discarding an
implementation, and begin with a bounded characterization slice before building
general machinery. The CLI oracle and reviewed obligation ledger contribute
only part of the durable Phoenix primitives; fuller specification, context
boundaries, regeneration provenance, and non-CLI evidence remain later work.

Relevant articles include [The Deletion Test](https://aicoding.leaflet.pub/3md5ftetaes2e),
[Evaluations Are the Real Codebase](https://aicoding.leaflet.pub/3mb526js42k26),
[The Implementation Remembers](https://aicoding.leaflet.pub/3mobohx4fq22x),
[The Regenerative Grain](https://aicoding.leaflet.pub/3mfai4nqg6224), and
[The Phoenix Primitives](https://aicoding.leaflet.pub/3mjfruwwuck2d).
