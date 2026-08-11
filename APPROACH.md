# Experimental approach

Phoenix will be developed by working backward from a real, conventionally built system rather than by designing a general generation platform from hypothetical requirements.

The phases below are deliberately sequential. Each phase should produce evidence before the next phase introduces new assumptions.

## Why Clojure

Clojure is an intentional part of the experiment, not merely an incidental implementation language.

- Immutable, data-oriented programming fits a functional core that receives commands and coeffects and returns events and effects.
- Effects, coeffects, interceptor chains, application requests, and intermediate representations can all remain explicit data rather than becoming framework-specific control flow.
- Small functions and a compact, regular language encourage the fine-grained composition we want generators to be able to exploit.
- Clojure's code-as-data character and simple syntax make generated artifacts comparatively direct to inspect, validate, transform, and evaluate.
- Clojure and ClojureScript offer a path to apply the same architectural ideas across CLI, server, Node, and browser environments.
- The REPL and the JVM and JavaScript ecosystems allow rapid experimentation without requiring us to build every runtime facility ourselves.

JVM Clojure is the tactical choice for the first reference because it reduces initial tooling and persistence overhead. The original inclination was to use ClojureScript across the stack, and that remains a target worth revisiting. The architectural experiment should distinguish Clojure-level ideas from assumptions tied specifically to the JVM host.

This experiment does not need to prove that Phoenix is language-independent. It should first discover whether the regenerative approach works well in the language and programming model that motivated it.

## 1. Build the reference system

Build the limited RealWorld application under `reference/` as a normal standalone JVM Clojure project. Establish an application architecture we would choose even if Phoenix did not exist.

During this phase:

- do not introduce Phoenix tooling or generated code;
- do not optimize implementation seams for later generation;
- use the functional-core, imperative-shell and interceptor ideas as working architectural hypotheses;
- build through small vertical behaviors, starting with the CLI;
- keep the application layer independent of its interface.

The phase ends when the agreed RealWorld slice works from a clean database and is credible as an ordinary application.

## 2. Freeze and characterize behavior

Capture the reference system through black-box contracts at its public boundaries. Build a reproducible reference artifact that can be invoked without exposing implementation details.

The contracts should describe externally observable behavior, not namespace structure, internal data representations, or the particular interceptor decomposition.

The phase ends when the contracts can distinguish working behavior from meaningful mutations and can run repeatedly against the frozen reference.

## 3. Extract and generalize the architecture

Study the completed reference implementation and separate reusable architectural mechanisms from RealWorld-specific behavior.

Potential reusable mechanisms include:

- commands and application requests;
- functional decisions;
- effects and coeffects;
- interceptor composition;
- interface adapters;
- persistence and other effect interpreters;
- assembly into a standalone project.

The generalized architecture should describe constraints and available mechanisms without encoding RealWorld concepts. It may be informed by the reference source; it should not merely rename or template the existing files.

The phase ends when the architecture can be explained independently of the reference application's domain.

## 4. Describe the application independently

Write durable, capability-oriented specifications from the reference behavior and product intent.

Specifications should describe outcomes, rules, invariants, boundaries, and meaningful examples. They should not translate Clojure functions, interceptor names, context-map paths, or source layout into prose.

The reference implementation remains hidden from implementation generators. Generators receive only the relevant application specification and generalized architecture.

The phase ends when the application description and behavioral contracts are sufficient to attempt one replacement without consulting reference source.

## 5. Build the minimum Phoenix required

Build only enough Phoenix machinery to generate a standalone replacement for one capability under the generalized architecture.

Phoenix is a build-time tool, not the application runtime. Its output must be an ordinary project with its own source, dependencies, tests, runtime, and startup commands.

Do not generalize ahead of observed needs. Let the first replacement reveal requirements for canonicalization, generation, evaluation, assembly, provenance, and regeneration.

## 6. Demonstrate regeneration

Run the same black-box contracts against the reference and generated systems. Behavioral equivalence matters; source and decomposition equivalence do not.

A successful experiment must go beyond producing one implementation:

1. generate a replacement;
2. verify it against the contracts;
3. remove the generated implementation;
4. generate another implementation from the conserved inputs;
5. verify the behavior again.

Expand to another capability only after this loop works.

## Context boundaries

Different agents have different legitimate access:

- The reference-development agent works only on the conventional reference system and public product documentation.
- An architecture-generalization agent may inspect the frozen reference after that phase begins.
- A Phoenix-development agent may see the generalized architecture, application specifications, and behavioral contracts.
- An implementation generator must not see the reference source or hidden evaluation evidence.

Agent instructions should be updated when the repository intentionally changes phases. Starting a new phase should use a fresh agent session so conversational history does not accidentally collapse these boundaries.

## Working principles

- Treat architectural ideas as hypotheses until the reference system applies pressure to them.
- Prefer evidence from running behavior over abstract completeness.
- Keep each phase independently understandable and reproducible.
- Avoid hosted, multi-user, and organizational concerns; this begins as a local tool for one developer.
- Preserve the distinction between application intent, architectural mechanism, generated implementation, and behavioral evidence.
