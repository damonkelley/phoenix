# Phoenix

An experiment in developing a regenerative software system from a real,
behaviorally characterized application, inspired by Chad Fowler's
[Phoenix Architecture writing on Leaflet](https://aicoding.leaflet.pub/).

See [`APPROACH.md`](APPROACH.md) for the staged reference, architecture
extraction, generation, and regeneration experiment.

## Current phase: freeze and characterize behavior

Phase 1 produced `reference/`, a conventional standalone JVM Clojure
implementation of a limited [RealWorld](https://realworld-docs.netlify.app/)
application. Its behavior is frozen at the `reference-phase-1` tag.

The frozen CLI can:

- register a user;
- log in;
- create an article;
- list the global feed;
- view an article;
- persist users and articles in SQLite.

Phase 2 captures that system through implementation-independent contracts at
its public CLI boundary. It will produce a reproducible reference artifact,
exercise it from clean databases, and establish evaluations capable of
detecting meaningful behavioral mutations.

The reference implementation is now treated as opaque during behavioral
characterization. Architecture generalization, Phoenix tooling, generation, and
a web interface remain outside this phase.

## Reference architecture

The application layer is independent of its interfaces. A functional core
accepts a command and resolved coeffects, then emits events and requested
effects as data. An imperative shell resolves coeffects and interprets effects.

An interceptor chain initially composes validation, coeffect resolution, domain
decision, and effect interpretation. Interceptors are an implementation
mechanism, not the human specification language.

## Isolation rule

Behavioral contracts must be derived from public product documentation and
observable execution, not from reference namespaces, source layout, internal
data structures, tests, or database structure. They must be able to evaluate a
replacement implementation through the same public boundary.
