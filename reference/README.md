# RealWorld reference application

This directory contains the independently runnable, conventional JVM Clojure
application completed during phase 1 of the experiment. It is intentionally
implemented without Phoenix tooling or generated code.

The reference behavior and implementation are frozen at the
`reference-phase-1` tag. Phase 2 treats this implementation as opaque and
characterizes it through separately maintained contracts at the public CLI
boundary. New behavioral evidence and regeneration machinery do not belong
here.

## Reproducible invocation

The frozen application source is the annotated tag `reference-phase-1`, which
resolves to commit `d8be109886efe75ee663548a59af218ff4986f5b`. The launcher and
these instructions are phase 2 reproducibility work layered on that frozen
source; no packaged artifact is built or committed.

Install [mise](https://mise.jdx.dev/) and provision the toolchain pinned in
[`.mise.toml`](.mise.toml): Temurin Java `21.0.12+8.0.LTS`, Clojure CLI
`1.12.5.1664`, and Babashka `1.12.218`. From the repository root, run:

```sh
mise install -C reference java clojure babashka
```

Run the complete implementation test suite and formatting check through the
pinned toolchain:

```sh
mise exec -C reference -- bb test
mise exec -C reference -- bb format:check
```

`bin/realworld` locates this project from its own path and resolves Java and
Clojure through the same mise configuration. It does not change the caller's
working directory. For example, this invokes the opaque command in a temporary
workspace and demonstrates persistence across two command processes:

```sh
reference_command="$(pwd)/reference/bin/realworld"
workspace=$(mktemp -d)

(
  cd "$workspace"
  "$reference_command" account register \
    --email smoke@example.com --password secret123
  "$reference_command" account login \
    --email smoke@example.com --password secret123
)

rm -rf "$workspace"
```

No separate build command is required. Application state remains scoped to the
working directory from which callers invoke the launcher.
