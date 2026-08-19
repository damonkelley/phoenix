# CLI behavioral evidence

This independent project is reserved for reviewed, implementation-independent
evidence at the application's public CLI boundary. Its first and only current
slice is account registration; the reviewed ledger is
[`obligations/registration.md`](obligations/registration.md), and the durable,
non-normative black-box record is
[`observations/registration.md`](observations/registration.md).

The boundary consists of target command arguments, exit status, standard
output, standard error, the caller-selected working directory, and state
observable through later public CLI invocations. A future evaluator must accept
a target executable without depending on its language, source layout, runtime,
or relationship to the frozen reference implementation.

Current equivalence is deliberately limited to scenarios that begin from clean
state. Separate target processes within one scenario must share state when run
from the same working directory, while different working directories select
independent state. Existing-data continuity is not in scope. Evidence must not
inspect target-created files, databases, source, or other internals.

No contract runner, invocation protocol, project runtime, test implementation,
or other contract machinery has been selected or configured yet. This project
currently contains only the reviewed obligation ledger and its separate
non-normative observation record.
