# Account-registration obligation ledger

**Ledger status:** reviewed and approved for the first clean-state
characterization slice

**Scope:** `account register` at the public CLI boundary only

This is a human-reviewable record of what the first contracts may conserve. It
is not a test DSL and does not select a runner, runtime, or implementation.

## Evidence and review policy

Permitted product provenance is limited to the public
[registration specification](../../reference/docs/specs/accounts/registration.md),
its directly linked [CLI result](../../reference/docs/adr/0001-cli-command-result-conventions.md),
[validation](../../reference/docs/adr/0002-validation-error-reporting.md),
[operational failure](../../reference/docs/adr/0003-operational-failure-classification.md),
[SQLite location](../../reference/docs/adr/0004-sqlite-database-location.md), and
[credential storage](../../reference/docs/adr/0005-password-credential-storage.md)
decisions, plus the repository [phase-2 handoff](../../PHASE2.md),
[README](../../README.md), and [approach](../../APPROACH.md).

Black-box observation provenance is the durable, non-normative
[registration observation report](../observations/registration.md). It records
102 invocations: each single-command probe and each multi-process sequence was
repeated three times from clean temporary working directories, without
inspecting state internals.

An observation does **not** become an obligation merely because it is stable or
easy to assert. Promotion requires a documented requirement or an explicit
reviewed reason recorded here.

### Classification and status

- **DPO** — durable product obligation.
- **OC** — public operational constraint.
- **BO** — black-box observation, not itself an obligation.
- **IR** — incidental detail that is deliberately not conserved.
- **UD** — unresolved decision.
- **EI** — excluded implementation detail.
- **Approved** items may inform the first contracts.
- **Accepted; evidence deferred** items remain obligations, but this CLI slice
  cannot establish them.

## Documented obligations approved for the first CLI slice

| ID | Status / class | Permitted provenance | Reviewed rationale | Intended evidence | Exclusions or unresolved points |
|---|---|---|---|---|---|
| REG-DOC-001 | Approved / DPO | [Registration: Command](../../reference/docs/specs/accounts/registration.md#command) | Registration is invoked through the public command arguments `account register --email EMAIL --password PASSWORD`. The executable itself remains target-configurable. | Invoke a supplied target directly with this documented argument form. | No executable basename, alternate flag syntax, flag ordering, repeated flags, or abbreviations are specified. |
| REG-DOC-002 | Approved / DPO | [Registration: Success](../../reference/docs/specs/accounts/registration.md#success) | A valid first registration creates an account and reports the logical output line `Success`. | Require status `0`, exactly the logical stdout line `Success`, and empty stderr. | No exact terminal newline or persisted representation is required by this CLI evidence. |
| REG-DOC-003 | Approved / OC | [ADR 0001](../../reference/docs/adr/0001-cli-command-result-conventions.md#decision) | Exit status and output channel are part of the public process protocol. | Success uses stdout/status `0`; validation and domain failures use stderr/status `1`; malformed usage uses stderr/status `2`. | Operational status `1` is accepted but its fault scenario is deferred below. |
| REG-DOC-004 | Approved / DPO | [Registration: Registration errors](../../reference/docs/specs/accounts/registration.md#registration-errors), [ADR 0002](../../reference/docs/adr/0002-validation-error-reporting.md#decision) | Users receive all applicable exact validation messages without order becoming behavior. Dependent constraints are suppressed when a required value is absent. | Compare stderr as an **unordered exact set of logical lines**: every expected message and no unexpected message. Require stdout empty and status `1`. | Do not require an exact terminal newline. Supplied-empty values remain unresolved in REG-UD-001. |
| REG-DOC-005 | Approved / DPO | [Registration: Email](../../reference/docs/specs/accounts/registration.md#email) | The practical ASCII email grammar is explicit product behavior. | Exercise allowed local characters, dot and domain-label boundaries, one-label and dotted domains, whitespace rejection, and ASCII-only addresses. | No undocumented maximum length, Unicode acceptance, or normalization rule is added. |
| REG-DOC-006 | Approved / DPO | [Registration: Email](../../reference/docs/specs/accounts/registration.md#email) | Registration validates syntax, not domain or mailbox existence. | A syntactically valid address under the reserved `.invalid` TLD succeeds from clean state. | No network or mailbox lookup is performed by the evidence. |
| REG-DOC-007 | Approved / DPO | [Registration: Email](../../reference/docs/specs/accounts/registration.md#email) | Email identity and uniqueness are case-insensitive. | Register mixed case, then retry another casing in a later process and require `Email is already taken`. | Original-case storage or display is not observable in this slice and is not inferred. |
| REG-DOC-008 | Approved / DPO | [Registration: Password](../../reference/docs/specs/accounts/registration.md#password) | A password is required, contains at least eight characters, contains no whitespace, and is not trimmed or normalized. | Cover seven/eight-character boundaries, whitespace combined with short length, and leading whitespace. | Use ASCII examples; Unicode length semantics remain unresolved in REG-UD-005. Credential persistence is deferred in REG-DEF-002. |
| REG-DOC-009 | Approved / DPO | [Registration: Rule evaluation](../../reference/docs/specs/accounts/registration.md#rule-evaluation) | Validation precedes uniqueness, so invalid input does not disclose a duplicate result. | Against an existing case-varied email, an invalid password reports only its applicable validation error. | Hashing timing and internal evaluation stages are excluded. |
| REG-DOC-010 | Approved / DPO | [Registration: Rule evaluation](../../reference/docs/specs/accounts/registration.md#rule-evaluation) | A failed registration must not create or reserve an account. | Follow an invalid attempt with a valid case-varied attempt; the latter must succeed. | No storage inspection is permitted. |
| REG-DOC-011 | Approved / DPO | [Registration: Success](../../reference/docs/specs/accounts/registration.md#success), [phase-2 scope](../../PHASE2.md#next-work), [ADR 0004](../../reference/docs/adr/0004-sqlite-database-location.md#consequences) | Account creation is persistent public behavior, including across separate command processes in one scenario. | Observe a successful registration through duplicate behavior from a later target process in the same working directory. | Clean-state scenarios only; existing-data inheritance is REG-UD-003. |
| REG-DOC-012 | Approved / OC | [ADR 0004](../../reference/docs/adr/0004-sqlite-database-location.md#decision) | The caller's current working directory publicly selects application state and therefore supplies the isolation boundary. | The same email can be registered independently in two concurrent clean working directories; each directory retains its own duplicate state across processes. | This proves cwd-visible state selection, not SQLite or a filename. Never inspect directory contents. |
| REG-DOC-013 | Approved / OC | [ADR 0001](../../reference/docs/adr/0001-cli-command-result-conventions.md#decision), observation [P19](../observations/registration.md#p19) | A missing option value is a representative malformed invocation, distinct from feature validation. | A dangling `--password` produces status `2`, empty stdout, and a non-empty stderr diagnostic. | Exact diagnostic wording and terminal newline are not contractual. |

## Black-box observations

These observations corroborate or delimit reviewed obligations; they have no
independent normative force.

| ID | Status / class | Permitted provenance | Observation and rationale | Intended evidence | Exclusions or unresolved points |
|---|---|---|---|---|---|
| REG-OBS-001 | Observed only / BO | Observations [P1](../observations/registration.md#p1) and [P21](../observations/registration.md#p21) | Canonical registration, including an eight-character password, returned `(0, "Success\n", "")`. | Corroborates REG-DOC-002 and the password boundary. | The observed LF is incidental. |
| REG-OBS-002 | Observed only / BO | Observations [P2–P4](../observations/registration.md#p2), [P6–P8](../observations/registration.md#p6), and [P22](../observations/registration.md#p22) | Required, length, whitespace, and combined-invalid cases returned the documented messages on stderr with status `1`; applicable independent errors were aggregated. | Corroborates REG-DOC-004 and REG-DOC-008. | Observed message order is not promoted. |
| REG-OBS-003 | Observed only / BO | Observations [P9–P18](../observations/registration.md#p9), [P20](../observations/registration.md#p20), and [P23](../observations/registration.md#p23) | The documented ASCII email boundaries, a one-label domain, and a reserved nonexistent domain behaved consistently with the specification. | Corroborates REG-DOC-005 and REG-DOC-006. | The probes do not invent length limits or broader email syntax. |
| REG-OBS-004 | Observed only / BO | Observation [P24](../observations/registration.md#p24) | Mixed-case duplicate detection persisted across processes, while invalid input against the duplicate reported validation only. | Corroborates REG-DOC-007, REG-DOC-009, and REG-DOC-011. | It does not expose stored casing or internal evaluation order. |
| REG-OBS-005 | Observed only / BO | Observation [P25](../observations/registration.md#p25) | A failed registration did not reserve the email; a valid case-varied retry succeeded and then became a duplicate. | Corroborates REG-DOC-010. | State was observed only through later public commands. |
| REG-OBS-006 | Observed only / BO | Observation [P26](../observations/registration.md#p26) | Separate processes shared duplicate state within each working directory, while two working directories remained independent. | Corroborates REG-DOC-011 and REG-DOC-012. | Only cwd-visible state selection was observed. |
| REG-OBS-007 | Observed only / BO | Observation [P19](../observations/registration.md#p19) | A dangling `--password` returned status `2`, empty stdout, and `Missing value for option --password\n` on stderr. | Supports the representative malformed-usage scenario in REG-DOC-013. | The exact text and LF are not promoted. |
| REG-OBS-008 | Observed only / BO | Observation [P5](../observations/registration.md#p5) | Supplied empty values produced the two required-value messages. | Records the behavior for a later decision. | Explicitly excluded from the first contracts by REG-UD-001. |

## Incidental details not conserved

| ID | Status / class | Permitted provenance | Reviewed rationale | Intended evidence | Exclusions or unresolved points |
|---|---|---|---|---|---|
| REG-INC-001 | Not conserved / IR | [Observations P1–P26](../observations/registration.md#single-command-probes); [ADR 0002](../../reference/docs/adr/0002-validation-error-reporting.md#decision) | Current validation-line ordering has no contractual basis, and the ADR explicitly excludes ordering. | Compare unordered logical lines. | No assertion on which error is printed first. |
| REG-INC-002 | Not conserved / IR | [Observations P1–P26](../observations/registration.md#single-command-probes) | Current output ends in LF, but exact terminal-newline bytes add no reviewed product value. | Compare logical line content while tolerating presence or absence of a terminal newline. | Extra or missing logical lines still fail. |
| REG-INC-003 | Not conserved / IR | Observation [P19](../observations/registration.md#p19); [ADR 0001](../../reference/docs/adr/0001-cli-command-result-conventions.md#decision) | `Missing value for option --password` is shared-harness wording, not a feature-defined message. | Require only malformed classification, status/channel behavior, and a non-empty diagnostic. | Do not snapshot or substring-match the wording. |

## Accepted obligations whose evidence is deferred

| ID | Status / class | Permitted provenance | Reviewed rationale | Intended evidence | Exclusions or unresolved points |
|---|---|---|---|---|---|
| REG-DEF-001 | Accepted; evidence deferred / OC | [ADR 0004](../../reference/docs/adr/0004-sqlite-database-location.md#decision) | SQLite durable persistence and the default `realworld.db` file in the process cwd are accepted documented operational constraints. They must not be silently generalized away merely because storage is behind the CLI. | Preserve for a later, separately reviewed non-CLI evidence mechanism. This first suite proves only cwd-visible state selection and process persistence. | The CLI suite must not list, open, query, or otherwise inspect the file or database. No evidence mechanism is selected here. |
| REG-DEF-002 | Accepted; evidence deferred / DPO | [Registration: Password](../../reference/docs/specs/accounts/registration.md#password), [ADR 0005](../../reference/docs/adr/0005-password-credential-storage.md#decision) | Only a salted, one-way password hash may persist; plaintext password persistence is forbidden. This accepted security obligation is invisible through registration's public CLI behavior. | Preserve for a later independent security-evidence mechanism capable of proving both hashing and absence of plaintext. | This CLI slice must not inspect storage or claim the obligation is proved. Exact Buddy compatibility is separately unresolved in REG-UD-002. |
| REG-DEF-003 | Accepted; evidence deferred / OC | [ADR 0003](../../reference/docs/adr/0003-operational-failure-classification.md#decision), [ADR 0001](../../reference/docs/adr/0001-cli-command-result-conventions.md#decision) | Operational failures owe a concise human-readable stderr message, status `1`, and no default stack trace. | Add evidence only if a target-independent, public-boundary way to induce an operational fault is reviewed. | Operational-fault injection is explicitly deferred; a SQLite-specific sabotage would improperly couple the first suite to internals. |

## Unresolved decisions

| ID | Status / class | Permitted provenance | Reviewed rationale | Intended evidence | Exclusions or unresolved points |
|---|---|---|---|---|---|
| REG-UD-001 | Unresolved / UD | [Registration rules](../../reference/docs/specs/accounts/registration.md#input-rules), observation [P5](../observations/registration.md#p5) | Documentation distinguishes required values from invalid present values but does not explicitly define whether a supplied empty argv value is absent. Observation alone is insufficient. | None until human review defines supplied-empty semantics. | Excluded from the first scenarios. |
| REG-UD-002 | Unresolved / UD | [ADR 0005](../../reference/docs/adr/0005-password-credential-storage.md#decision), [phase-2 clean-state scope](../../PHASE2.md#next-work) | Buddy Hashers `:bcrypt+sha512` is the documented reference algorithm, but exact replacement compatibility is not established as a clean-state CLI obligation. | Revisit if existing-data continuity is later required; then select suitable non-CLI evidence. | Salted one-way hashing and no plaintext persistence remain accepted regardless of this decision. |
| REG-UD-003 | Unresolved / UD | [Phase-2 clean-state scope](../../PHASE2.md#next-work) | Current equivalence starts clean and therefore cannot establish whether a replacement must read existing reference data. | No evidence in this slice. | Do not imply migration, schema, file-format, or hash compatibility. |
| REG-UD-004 | Unresolved / UD | [Registration: Command](../../reference/docs/specs/accounts/registration.md#command) | Only one argument form is documented. Behavior for reordered flags, `--name=value`, repeated flags, unknown flags, and abbreviations is not specified. | Use only the documented form in the first scenarios. | No malformed-usage generalization beyond REG-DOC-013. |
| REG-UD-005 | Unresolved / UD | [Registration: Password](../../reference/docs/specs/accounts/registration.md#password) | “Characters” does not define Unicode counting semantics, although non-normalization and whitespace rules are explicit. | Use ASCII passwords for the first slice. | Do not infer byte, code-point, or grapheme counting. |

## Excluded implementation details

| ID | Status / class | Permitted provenance | Reviewed rationale | Intended evidence | Exclusions or unresolved points |
|---|---|---|---|---|---|
| REG-EXC-001 | Excluded / EI | [ADR 0005](../../reference/docs/adr/0005-password-credential-storage.md#decision) | Coeffect placement, imperative-shell resolution, account-decision inputs, and effect contents describe reference machinery rather than public registration outcomes. | None. | Do not encode these structures in contracts or replacement requirements. |
| REG-EXC-002 | Excluded / EI | [ADR 0005 consequences](../../reference/docs/adr/0005-password-credential-storage.md#consequences) | Hashing duplicate attempts and its timing follow the current staging mechanism, not a reviewed public result. | None; do not use timing probes. | Security obligations remain in REG-DEF-002. |
| REG-EXC-003 | Excluded / EI | [ADR 0004](../../reference/docs/adr/0004-sqlite-database-location.md), [phase-2 evidence boundary](../../PHASE2.md#evidence-boundary) | Schema, tables, columns, migrations, database contents, and initialization mechanism are internal. | None in CLI characterization. | This does not cancel the accepted SQLite/location constraints in REG-DEF-001. |
| REG-EXC-004 | Excluded / EI | [Approach: phase 2](../../APPROACH.md#2-freeze-and-characterize-behavior), [phase-2 evidence boundary](../../PHASE2.md#evidence-boundary) | Source layout, namespaces, interceptors, language, runtime, and target packaging are not public registration behavior. | Evaluate future targets only at the configured public boundary. | No source, tests, migrations, database, history/diffs, or sibling repository inspection. |

## Approved first contract scenarios

The following scenarios are approved for later implementation, unchanged
across a configured opaque reference command and independent replacement
targets. Every scenario begins in a fresh clean working directory. Every step
in a sequence launches a separate target process, and state is observed only by
later public registration commands.

1. **Successful registration and process result** — a canonical email and an
   exactly eight-character ASCII password produce status `0`, exactly the
   logical stdout line `Success`, and empty stderr.
2. **Documented email grammar and offline validation** — independently cover a
   canonical address, a one-label domain, all allowed local-part characters,
   an internal domain hyphen, and a reserved `.invalid` domain as accepted;
   cover missing `@`, leading/trailing/repeated local dot,
   leading/trailing domain-label hyphen, an empty domain label, whitespace, and
   a non-ASCII address as rejected with the documented logical line.
3. **Password rules and complete validation reporting** — cover email and
   password absence individually and together, seven/eight-character password
   boundaries, short-plus-whitespace aggregation, leading whitespace without
   trimming, and invalid-email-plus-short-password aggregation. Validation
   output is an unordered exact set of logical lines, with no terminal-newline
   requirement.
4. **Failure atomicity, uniqueness, and validation precedence** — an invalid
   first attempt does not reserve the email; a valid differently cased retry
   succeeds; an invalid-password retry against that account reports only the
   password validation error; a valid case-varied retry reports exactly
   `Email is already taken`.
5. **Process persistence and cwd isolation** — duplicate state survives across
   separate processes in one working directory, while the same email can be
   registered independently in a second clean working directory.
6. **Representative malformed invocation** — a dangling `--password` produces
   status `2`, empty stdout, and a non-empty stderr diagnostic, without fixing
   its exact words or terminal newline.

## Explicitly deferred from this slice

- selecting or implementing a runner, runtime, invocation protocol, test suite,
  or faulty candidate;
- operational-fault injection;
- file, SQLite, database, hash, or plaintext-storage inspection;
- a non-CLI evidence mechanism for accepted storage and security obligations;
- exact malformed-usage text and exact terminal-newline bytes;
- supplied-empty semantics;
- existing-data continuity and exact Buddy algorithm compatibility;
- alternate argument forms, Unicode password length semantics, timing, and
  performance;
- login, articles, feeds, and every feature other than account registration;
- Phoenix architecture, generation, regeneration, and tooling.
