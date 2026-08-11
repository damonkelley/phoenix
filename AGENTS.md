# Repository instructions

Read `README.md` before making changes. Also follow any more specific
`AGENTS.md` found in the directory being changed.

## Purpose

This repository explores regenerative software development by first
establishing real application behavior and later discovering what Phoenix
machinery is required. Do not assume that the current directory structure
represents a final Phoenix product architecture.

## Working approach

- Work incrementally and discuss consequential design choices with the user.
- Do not introduce speculative top-level structure, frameworks, or platform
  concerns.
- Keep independently runnable projects conventional and self-contained.
- Preserve public behavioral evidence separately from implementation details
  when that distinction becomes relevant.
- Treat recorded direction as a hypothesis to test, not an immutable roadmap.

## Isolation

Do not inspect, read, search, copy, or derive implementation ideas from sibling
repositories unless the user explicitly requests it. Sibling projects are
deliberately excluded from this repository's agent context.
