# 0005: Password credential storage

## Status

Accepted

## Context

Account registration receives a plaintext password that will later be used to
authenticate login attempts. Persisting that password directly would expose
user credentials if the database were read and would make the persistence
representation an unsafe application default.

Secure password hashing requires a random salt and deliberately expensive
computation. That work is nondeterministic and does not belong in the
functional account decision.

## Decision

Persist only a salted, one-way hash derived from an account password. Never
persist the plaintext password.

The account command declares password hashing as a coeffect. The imperative
shell resolves that coeffect before the account decision, and the account
creation effect contains the resulting password hash instead of the supplied
password.

The reference implementation uses Buddy Hashers with the `:bcrypt+sha512`
algorithm explicitly selected. The encoded hash carries the information needed
for future password verification.

## Consequences

The SQLite accounts table stores a password hash and has no plaintext password
column. A future login capability must verify supplied passwords through a
hashing adapter rather than compare stored strings directly.

Hash derivation is intentionally computationally expensive and occurs whenever
a valid registration reaches coeffect resolution, including when the email is
already registered. Avoiding that work would require staged coeffect resolution
and is not currently justified.

Changing the hashing algorithm or its parameters may require verifying and
upgrading existing hashes during future login behavior or through a migration.
