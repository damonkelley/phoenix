# Phoenix

An experiment in developing a regenerative software system from a real,
behaviorally characterized application.

## Current phase: reference application

For now this repository contains only `reference/`, a conventional standalone
implementation of a limited [RealWorld](https://realworld-docs.netlify.app/)
application. We will not introduce Phoenix-specific structure until the
reference behavior is established and frozen.

The initial reference milestone is a JVM Clojure CLI application that can:

- register a user;
- log in;
- create an article;
- list the global feed;
- view an article;
- persist users and articles in SQLite.

A web interface comes after the CLI behavior is established.

## Reference architecture

The application layer is independent of its interfaces. A functional core
accepts a command and resolved coeffects, then emits events and requested
effects as data. An imperative shell resolves coeffects and interprets effects.

An interceptor chain initially composes validation, coeffect resolution, domain
decision, and effect interpretation. Interceptors are an implementation
mechanism, not the human specification language.

## Isolation rule

The reference must be built as an ordinary application without consulting or
copying implementation code from `phoenix-lab` or Chad Fowler's Phoenix
repository. Its public behavior will later become the evidence used for
regeneration experiments.
