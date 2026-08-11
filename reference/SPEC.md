# RealWorld reference — initial handoff

This is a lightweight handoff for building the reference application. It is not a Phoenix capability specification and should not be treated as a proposed future specification format.

## Purpose

Build a small, credible RealWorld application conventionally. Its observable behavior will later provide evidence for experiments in regenerating parts of a working system.

Do not design this implementation to be easy for Phoenix to reproduce. Build the application as we would normally want to build and maintain it.

## Initial product slice

Create a standalone JVM Clojure command-line application backed by SQLite. The initial slice should eventually let a user:

- register;
- log in;
- create an article;
- list articles in the global feed;
- view an article.

Use the public [RealWorld documentation](https://realworld-docs.netlify.app/) to clarify expected product behavior. Keep the first slice deliberately smaller than the complete RealWorld application.

## Application shape

The application layer should be independent of its interface so that a web adapter can follow the CLI without replacing application behavior.

Use a functional-core, imperative-shell model:

- the functional core accepts a command and resolved coeffects;
- it returns events and requested effects as data;
- the imperative shell resolves coeffects and interprets effects;
- external state and operations do not become ambient dependencies of the core.

Use the interceptor pattern to compose small implementation pieces. The initial application pipeline we have in mind is:

```text
validation
→ coeffect resolution
→ domain decision
→ effect interpretation
```

This is a starting shape, not a requirement to encode implementation structure in product descriptions. Interceptors are useful implementation machinery because small pieces can be composed; they are not intended to become a Markdown representation of Clojure code.

## Evidence

Develop ordinary unit and integration tests as useful, but also preserve behavior at public boundaries. Black-box CLI tests should invoke the application as a user would and avoid depending on internal namespaces or data structures.

The reference behavior should eventually be reproducible from a clean database and stable enough to compare another implementation against it.

## Not in the initial slice

- Web UI or HTTP API
- Comments
- Favorites
- Following
- Personalized feeds
- Full RealWorld compatibility
- Phoenix tooling, generated code, or future Phoenix repository design
- ClojureScript build and packaging concerns

ClojureScript across the stack remains an option worth revisiting after the JVM reference teaches us what the application needs.

## Working approach

Build incrementally and riff on design decisions with the user. Do not implement the whole slice from this handoff in one pass. Start by agreeing on the smallest useful vertical behavior, implement it conventionally, and let concrete pressure refine the architecture.

Important choices remain open, including CLI syntax, authentication representation, interceptor library versus a small local implementation, database library and schema, error representation, and the exact public contract tests.
