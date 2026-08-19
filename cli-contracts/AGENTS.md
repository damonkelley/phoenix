# CLI behavioral evidence instructions

Read this project's `README.md` and the applicable reviewed ledger under
`obligations/` before making changes.

## Current scope

The only approved characterization slice is account registration. Do not add
login, article, generation, architecture, or Phoenix behavior without first
creating and reviewing the corresponding obligation evidence.

## Isolation

Treat every target command as opaque. Derive obligations only from the public
feature specifications under `../reference/docs/specs/`, the ADRs they link,
and observable CLI execution. Do not inspect `../reference/src/`,
`../reference/test/`, migrations, databases, target-created files, or
implementation-revealing history and diffs.

Keep normative obligations under `obligations/` and non-normative black-box
observations under `observations/`. Repeatable observation alone does not make a
behavior contractual.

## Contract work

- Accept a configured target executable; never depend on reference namespaces,
  language, source layout, or storage representation.
- Run scenarios from clean temporary working directories and observe state only
  through later public commands.
- Preserve exit status, stdout, and stderr as separate observations.
- Model intentional nondeterminism and explicitly non-contractual formatting
  without snapshots of incidental output.
- Keep the project conventional, independently runnable, and limited to the
  machinery required by reviewed obligations.
