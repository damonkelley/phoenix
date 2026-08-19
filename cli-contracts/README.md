# CLI behavioral evidence

This independent Babashka project evaluates the reviewed account-registration
contract at an opaque executable boundary. The normative scope is the approved
[`obligations/registration.md`](obligations/registration.md) ledger;
[`observations/registration.md`](observations/registration.md) remains
non-normative evidence.

The runner observes only argv, process exit status, stdout, stderr, process cwd,
and state exposed by later public commands. It never lists or reads
target-created workspace contents.

## Provisioning

From the repository root, install the independently pinned contract runtime:

```sh
mise install -C cli-contracts babashka
```

To evaluate the repository's opaque reference command, provision that target's
separately pinned tools as documented by the phase handoff:

```sh
mise install -C reference java clojure babashka
```

There is no reference-specific default in the runner. Any candidate must be a
regular executable file and must implement the documented CLI protocol.

## Invocation

Run these exact commands from the repository root:

```sh
cd cli-contracts
mise exec -- bb test ../reference/bin/realworld
mise exec -- bb verify-faulty
```

The task interface is:

```text
bb test /absolute/or/relative/path/to/target
bb verify-faulty
```

`bb test` accepts exactly one target executable argument and resolves it to an
absolute path before creating or entering scenario workspaces. Targets are
launched directly with argv; no shell command interpolation is used.

## Implemented scenarios

The suite implements only the ledger's six approved scenarios:

1. successful registration and process result;
2. documented email grammar and offline validation;
3. password rules and complete validation reporting;
4. failure atomicity, case-insensitive uniqueness, and validation precedence;
5. persistence across processes and cwd-selected isolation; and
6. one representative malformed invocation.

Every scenario starts in fresh temporary state, every sequence step is a
separate process, and the cwd-isolation scenario uses two concurrently existing
clean directories. Cleanup runs after success, assertion failure, or exception.
Stdout, stderr, and exit status remain separate observations.

Documented output is compared as logical lines without trimming or other text
normalization. Validation lines are an unordered exact multiset: duplicates and
extra lines fail. Only the presence or absence of one terminal LF or CRLF is
tolerated.

## Discriminatory candidates

`faulty/` contains only three standalone ledger-derived substitutes:

- `always-success` ignores invalid and malformed outcomes;
- `first-error-only` omits all but the first applicable validation error; and
- `case-sensitive-identity-persistence` persists identity across processes but
  incorrectly treats email casing as significant.

`bb verify-faulty` first proves each candidate is executable and passes the
canonical success scenario, then applies the relevant real contract scenario
and requires rejection for the named fault rather than a launch failure. The
candidates do not invoke or wrap the reference.

## Explicitly unproven or deferred

This CLI slice does **not** prove or assert:

- SQLite storage, the `realworld.db` filename, schema, exact persisted values,
  or any target-created file contents;
- salted one-way password hashing, absence of persisted plaintext, or exact
  Buddy hash compatibility;
- operational-failure behavior, because no target-independent fault injection
  has been approved;
- supplied-empty semantics, Unicode password length semantics, alternate flag
  forms or ordering, exact malformed diagnostic text, or exact terminal-newline
  bytes;
- existing-data continuity, migration or file-format compatibility;
- timing, performance, email normalization beyond case-insensitive identity,
  or stored email casing; or
- login, articles, feeds, architecture extraction, generation, regeneration,
  or any Phoenix tooling.

The accepted SQLite/location, credential-security, and operational-failure
obligations remain deferred for separately reviewed evidence; they are not
silently generalized away by this suite.
