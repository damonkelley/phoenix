---
name: property-based-testing
description: Conventions for writing property-based tests in the RealWorld reference application using test.check, Malli schemas, and Lazytest. Use when adding or reviewing generative tests for commands, schemas, or application behavior.
---

# Property-based testing

Property tests in this project generate inputs from the same Malli schemas the
application uses for validation. The generator and the validator must never
drift apart, so both always derive from one schema.

All Malli usage stays behind `realworld.schema`:

```clojure
(schema/generator schema)  ; test.check generator for a schema
(schema/valid? schema value)
(schema/validate schema value) ; nil or a set of public error messages
```

Do not require `malli.*` namespaces in tests or application code.

## When to write a property test

Write a property test when there is an invariant over a whole input space:

- every schema-valid command is accepted;
- data flows losslessly from command to events and effects;
- every input is either accepted or rejected, never a third outcome;
- rejected inputs never produce effects.

Keep at least one concrete example test alongside each property. The example
documents exact expected values (`alice@example.com`); the property covers the
space. Neither replaces the other.

Do not write properties that restate the generator ("generated values
validate") — those are tautologies.

## The classification property

The highest-value property for a command is the total classification:

> Every command is either accepted with its effects interpreted, or rejected
> with validation errors and no effects. Nothing else.

Generate inputs spanning the valid *and* invalid regions by mutating
schema-valid values:

```clojure
(def mutations
  {:remove-email      #(update % :realworld.command/parameters
                               dissoc :realworld.account/email)
   :remove-parameters #(dissoc % :realworld.command/parameters)
   :unchanged         identity})

(def command-generator
  (generators/let [command (schema/generator account/RegisterCommand)
                   mutate (generators/elements (vals mutations))]
    (mutate command)))
```

Then branch the assertion on `schema/valid?`:

```clojure
(properties/for-all [command command-generator]
  (if (schema/valid? account/RegisterCommand command)
    (accepted? (dispatch command))
    (rejected-without-effects? (dispatch command))))
```

When adding validation rules to a schema, add mutations that violate the new
rules (too-short password, malformed email) so the invalid region stays
covered.

## Self-describing failures

Avoid bare `and` chains: a failing conjunction does not say which clause
failed. Compare one map so counterexamples identify the violated clause:

```clojure
(= {:outcome     :ok
    :event-email (:realworld.account/email parameters)
    :created     parameters}
   {:outcome     (:realworld.response/outcome response)
    :event-email (-> response :realworld.response/events
                     first :realworld.account/email)
    :created     (::effect.create result)})
```

## Running properties under Lazytest

Wrap `quick-check` so the full result — including the reproducing `:seed` —
appears in failure output:

```clojure
(defn check! [property]
  (let [result (test.check/quick-check 100 property)]
    (expect (:pass? result) (pr-str result))))
```

Use one `it` per property with a behavioral description:

```clojure
(it "either registers an account or rejects the command with errors"
  (check! ...))
```

100 cases is the default for pure in-memory dispatch. Increase only when
generators become richer or the property guards subtle boundaries.

To reproduce a failure, rerun with the reported seed:

```clojure
(test.check/quick-check 100 property :seed 1723456789)
```

## Boundaries

- Properties dispatch through `realworld.application`; never call command
  handlers directly.
- Use fake coeffect resolvers and context-in/context-out effect interpreters;
  observe effects through keys the fakes assoc into the context.
- Application-machinery properties belong in `application_test.clj` with
  synthetic commands; capability properties belong in the capability's test
  namespace with its real command definitions.
