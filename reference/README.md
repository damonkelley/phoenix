# RealWorld reference application

This directory contains the independently runnable, conventional JVM Clojure
application completed during phase 1 of the experiment. It is intentionally
implemented without Phoenix tooling or generated code.

The reference behavior and implementation are frozen at the
`reference-phase-1` tag. Phase 2 treats this implementation as opaque and
characterizes it through separately maintained contracts at the public CLI
boundary. A reproducible artifact may be packaged from this project, but new
behavioral evidence and regeneration machinery do not belong here.
