# Account-registration black-box observations

**Evidence status:** durable, non-normative observation record

**Scope:** `account register` at the public CLI boundary only

These observations corroborate documented obligations and surface questions for
ledger review. They do not become obligations, resolve open decisions, or
select contract assertions merely because they were repeatable. Normative
status remains solely with the reviewed
[account-registration obligation ledger](../obligations/registration.md).

## Probe method and evidence boundary

An isolated planner agent produced these results by executing the documented
opaque reference command and observing only:

- the exact arguments passed after the executable;
- the caller-selected current working directory;
- process exit status, standard output, and standard error; and
- state visible through later public CLI invocations.

The command was treated as opaque. The planner did not inspect the reference
implementation, tests, migrations, database, internal state, source history or
diffs, or sibling repositories. Temporary working-directory contents were not
inspected. This durable report transcribes the planner's recorded results; no
new probes were run while creating it.

Arguments below are represented as JSON-style argv arrays, excluding the
configured executable itself. Results use `(exit, stdout, stderr)`, with `\n`
representing the observed LF byte.

## Repetition and cleanup

Each single-command probe P1–P23 was run three times, with every run starting in
a new clean temporary working directory. Each complete multi-process sequence
P24–P26 was also repeated three times from clean temporary state. Every step in
a sequence launched a separate process. P26 used two clean working directories
that existed concurrently.

All recorded results were identical across the three repetitions. The planner
reported 102 command invocations in total. All temporary probe workspaces were
removed, and its final cleanup check found zero probe workspaces remaining.

## Single-command probes

| Probe | Exact arguments after executable | Observed `(exit, stdout, stderr)` |
|---|---|---|
| <a id="p1"></a>P1 | `["account", "register", "--email", "alice@example.com", "--password", "secret123"]` | `(0, "Success\n", "")` |
| <a id="p2"></a>P2 | `["account", "register"]` | `(1, "", "Email is required\nPassword is required\n")` |
| <a id="p3"></a>P3 | `["account", "register", "--password", "secret123"]` | `(1, "", "Email is required\n")` |
| <a id="p4"></a>P4 | `["account", "register", "--email", "alice@example.com"]` | `(1, "", "Password is required\n")` |
| <a id="p5"></a>P5 | `["account", "register", "--email", "", "--password", ""]` | `(1, "", "Email is required\nPassword is required\n")` |
| <a id="p6"></a>P6 | `["account", "register", "--email", "alice@example.com", "--password", "1234567"]` | `(1, "", "Password must be at least 8 characters\n")` |
| <a id="p7"></a>P7 | `["account", "register", "--email", "alice@example.com", "--password", "abc def"]` | `(1, "", "Password must be at least 8 characters\nPassword must not contain whitespace\n")` |
| <a id="p8"></a>P8 | `["account", "register", "--email", "alice@example.com", "--password", " secret123"]` | `(1, "", "Password must not contain whitespace\n")` |
| <a id="p9"></a>P9 | `["account", "register", "--email", "a@localhost", "--password", "secret123"]` | `(0, "Success\n", "")` |
| <a id="p10"></a>P10 | `["account", "register", "--email", "A.z_9%+-@Sub.Example-Domain.com", "--password", "secret123"]` | `(0, "Success\n", "")` |
| <a id="p11"></a>P11 | `["account", "register", "--email", ".alice@example.com", "--password", "secret123"]` | `(1, "", "Email is invalid\n")` |
| <a id="p12"></a>P12 | `["account", "register", "--email", "alice.@example.com", "--password", "secret123"]` | `(1, "", "Email is invalid\n")` |
| <a id="p13"></a>P13 | `["account", "register", "--email", "alice..x@example.com", "--password", "secret123"]` | `(1, "", "Email is invalid\n")` |
| <a id="p14"></a>P14 | `["account", "register", "--email", "alice@-example.com", "--password", "secret123"]` | `(1, "", "Email is invalid\n")` |
| <a id="p15"></a>P15 | `["account", "register", "--email", "alice@example-.com", "--password", "secret123"]` | `(1, "", "Email is invalid\n")` |
| <a id="p16"></a>P16 | `["account", "register", "--email", "alice@example..com", "--password", "secret123"]` | `(1, "", "Email is invalid\n")` |
| <a id="p17"></a>P17 | `["account", "register", "--email", "alice @example.com", "--password", "secret123"]` | `(1, "", "Email is invalid\n")` |
| <a id="p18"></a>P18 | `["account", "register", "--email", "álîçé@example.com", "--password", "secret123"]` | `(1, "", "Email is invalid\n")` |
| <a id="p19"></a>P19 | `["account", "register", "--email", "alice@example.com", "--password"]` | `(2, "", "Missing value for option --password\n")` |
| <a id="p20"></a>P20 | `["account", "register", "--email", "nobody@definitely-not-a-mailbox.invalid", "--password", "secret123"]` | `(0, "Success\n", "")` |
| <a id="p21"></a>P21 | `["account", "register", "--email", "alice@example.com", "--password", "12345678"]` | `(0, "Success\n", "")` |
| <a id="p22"></a>P22 | `["account", "register", "--email", "not-an-email", "--password", "short"]` | `(1, "", "Email is invalid\nPassword must be at least 8 characters\n")` |
| <a id="p23"></a>P23 | `["account", "register", "--email", " alice@example.com", "--password", "secret123"]` | `(1, "", "Email is invalid\n")` |

## Multi-process sequences

### P24: case-insensitive identity and validation precedence

<a id="p24"></a>

All four steps used one clean working directory, with a separate process for
each step.

| Step | Exact arguments after executable | Observed `(exit, stdout, stderr)` |
|---|---|---|
| 1 | `["account", "register", "--email", "Alice@Example.com", "--password", "secret123"]` | `(0, "Success\n", "")` |
| 2 | `["account", "register", "--email", "alice@example.com", "--password", "secret123"]` | `(1, "", "Email is already taken\n")` |
| 3 | `["account", "register", "--email", "ALICE@EXAMPLE.COM", "--password", "short"]` | `(1, "", "Password must be at least 8 characters\n")` |
| 4 | `["account", "register", "--email", "ALICE@EXAMPLE.COM", "--password", "secret123"]` | `(1, "", "Email is already taken\n")` |

### P25: failure atomicity

<a id="p25"></a>

All three steps used one clean working directory, with a separate process for
each step.

| Step | Exact arguments after executable | Observed `(exit, stdout, stderr)` |
|---|---|---|
| 1 | `["account", "register", "--email", "new@example.com", "--password", "short"]` | `(1, "", "Password must be at least 8 characters\n")` |
| 2 | `["account", "register", "--email", "NEW@EXAMPLE.COM", "--password", "secret123"]` | `(0, "Success\n", "")` |
| 3 | `["account", "register", "--email", "new@example.com", "--password", "secret123"]` | `(1, "", "Email is already taken\n")` |

### P26: current-working-directory isolation

<a id="p26"></a>

Clean working directories A and B existed concurrently. Each step launched a
separate process in the indicated working directory.

| Step | Working directory | Exact arguments after executable | Observed `(exit, stdout, stderr)` |
|---|---|---|---|
| 1 | A | `["account", "register", "--email", "same@example.com", "--password", "secret123"]` | `(0, "Success\n", "")` |
| 2 | B | `["account", "register", "--email", "same@example.com", "--password", "secret123"]` | `(0, "Success\n", "")` |
| 3 | A | `["account", "register", "--email", "same@example.com", "--password", "secret123"]` | `(1, "", "Email is already taken\n")` |
| 4 | B | `["account", "register", "--email", "same@example.com", "--password", "secret123"]` | `(1, "", "Email is already taken\n")` |
