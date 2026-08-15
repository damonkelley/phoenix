(ns realworld.account-test
  (:require [lazytest.core :refer [defdescribe describe expect it]]
            [realworld.application :as application]))

(def registration-command
  {:realworld.command/name       :realworld.account/register
   :realworld.command/parameters {:realworld.account/email    "alice@example.com"
                                  :realworld.account/password "secret123"}})

(defdescribe account
  (describe "registration"
    (it "registers an account with valid parameters"
      (let [context (application/context
                      {:command-definitions application/command-definitions
                       :coeffect-resolvers  {:realworld.uuid/generate (fn [] "id")}
                       :effect-interpreters {:realworld.repository/create (fn [context account]
                                                                            (assoc context ::effect.create account))}})
            result (application/dispatch context registration-command)]

        (expect (= {:realworld.response/outcome :ok
                    :realworld.response/data    {:realworld.account/id "id"}
                    :realworld.response/events  [{:realworld.event/type    :realworld.account/registered
                                                  :realworld.account/id    "id"
                                                  :realworld.account/email "alice@example.com"}]
                    :realworld.response/effects [[:realworld.repository/create
                                                  {:realworld.account/email    "alice@example.com"
                                                   :realworld.account/password "secret123"}]]}
                   (:realworld.application/response result)))
        (expect (= {:realworld.account/email    "alice@example.com"
                    :realworld.account/password "secret123"}
                   (::effect.create result)))))

    (it "normalizes the email without changing the password"
      {:skip true}
      (expect false))

    (it "rejects missing required parameters"
      {:skip true}
      (expect false))

    (it "reports all applicable validation errors"
      {:skip true}
      (expect false))

    (it "rejects an email that is already registered"
      {:skip true}
      (expect false))))
