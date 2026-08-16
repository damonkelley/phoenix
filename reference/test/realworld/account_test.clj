(ns realworld.account-test
  (:require [clojure.test.check :as test.check]
            [clojure.test.check.generators :as generators]
            [clojure.test.check.properties :as properties]
            [lazytest.core :refer [defdescribe describe expect it]]
            [realworld.account :as account]
            [realworld.application :as application]
            [realworld.schema :as schema]))

(def registration-command
  {:realworld.command/name       :realworld.account/register
   :realworld.command/parameters {:realworld.account/email    "alice@example.com"
                                  :realworld.account/password "secret123"}})

(def coeffect-resolvers
  {:realworld.uuid/generate (fn [] "id")})

(def effect-interpreters
  {:realworld.repository/create
   (fn [context account]
     (assoc context ::effect.create account))})

(defn test-context
  ([]
   (test-context {}))
  ([options]
   (application/context
     {:command-definitions account/command-definitions
      :coeffect-resolvers
      (merge coeffect-resolvers (:coeffect-resolvers options))
      :effect-interpreters
      (merge effect-interpreters (:effect-interpreters options))})))

(defn check! [property]
  (let [result (test.check/quick-check 100 property)]
    (expect (:pass? result) (pr-str result))))

(def mutations
  {:blank-email         #(assoc-in %
                                   [:realworld.command/parameters
                                    :realworld.account/email]
                                   " \t")
   :invalid-email       #(assoc-in %
                                   [:realworld.command/parameters
                                    :realworld.account/email]
                                   "alice..smith@example.com")
   :remove-email        #(update % :realworld.command/parameters
                                 dissoc :realworld.account/email)
   :remove-password     #(update % :realworld.command/parameters
                                 dissoc :realworld.account/password)
   :remove-parameters   #(dissoc % :realworld.command/parameters)
   :short-password      #(assoc-in %
                                   [:realworld.command/parameters
                                    :realworld.account/password]
                                   "short")
   :unchanged           identity
   :whitespace-password #(assoc-in %
                                   [:realworld.command/parameters
                                    :realworld.account/password]
                                   "secret 123")})

(def register-command-generator
  "Commands spanning the valid and invalid regions of RegisterCommand."
  (generators/let [command (schema/generator account/RegisterCommand)
                   mutate (generators/elements (vals mutations))]
    (mutate command)))

(defdescribe account
  (describe "registration"
    (it "registers an account with valid parameters"
      (let [result (application/dispatch (test-context) registration-command)]
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

    (it "registers every command allowed by its schema"
      (check!
       (properties/for-all [command (schema/generator account/RegisterCommand)]
                           (let [result (application/dispatch (test-context) command)
                                 parameters (:realworld.command/parameters command)
                                 response (:realworld.application/response result)]
                             (= {:outcome     :ok
                                 :event-email (:realworld.account/email parameters)
                                 :created     parameters}
                                {:outcome     (:realworld.response/outcome response)
                                 :event-email (-> response
                                                  :realworld.response/events
                                                  first
                                                  :realworld.account/email)
                                 :created     (::effect.create result)})))))

    (it "either registers an account or rejects the command with errors"
      (check!
       (properties/for-all [command register-command-generator]
                           (let [result (application/dispatch (test-context) command)
                                 response (:realworld.application/response result)
                                 created (::effect.create result)]
                             (if (schema/valid? account/RegisterCommand command)
                               (and (= :ok (:realworld.response/outcome response))
                                    (= (:realworld.command/parameters command) created))
                               (and (= :error (:realworld.response/outcome response))
                                    (= :validation (get-in response [:realworld.response/data
                                                                     :realworld.error/type]))
                                    (seq (get-in response [:realworld.response/data
                                                           :realworld.error/messages]))
                                    (nil? created)))))))

    (it "rejects registration without an email and password"
      (let [result (application/dispatch
                    (test-context)
                    {:realworld.command/name       :realworld.account/register
                     :realworld.command/parameters {}})]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type :validation
                     :realworld.error/messages
                     #{"Email is required" "Password is required"}}}
                   (:realworld.application/response result)))
        (expect (nil? (::effect.create result)))))

    (it "treats blank required values as missing"
      (let [result (application/dispatch
                    (test-context)
                    {:realworld.command/name :realworld.account/register
                     :realworld.command/parameters
                     {:realworld.account/email    " \t"
                      :realworld.account/password ""}})]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type :validation
                     :realworld.error/messages
                     #{"Email is required" "Password is required"}}}
                   (:realworld.application/response result)))
        (expect (nil? (::effect.create result)))))

    (it "rejects malformed email addresses"
      (doseq [email ["alice.example.com"
                     ".alice@example.com"
                     "alice.@example.com"
                     "alice..smith@example.com"
                     "ali!ce@example.com"
                     "alice@-example.com"
                     "alice@example-.com"
                     "alice@exam ple.com"
                     " alice@example.com"
                     "alice@example.com "]]
        (let [result (application/dispatch
                      (test-context)
                      {:realworld.command/name :realworld.account/register
                       :realworld.command/parameters
                       {:realworld.account/email    email
                        :realworld.account/password "secret123"}})]
          (expect (= {:realworld.response/outcome :error
                      :realworld.response/data
                      {:realworld.error/type     :validation
                       :realworld.error/messages #{"Email is invalid"}}}
                     (:realworld.application/response result))
                  (str "email: " (pr-str email)))
          (expect (nil? (::effect.create result))))))

    (it "reports all applicable validation errors"
      (let [result (application/dispatch
                    (test-context)
                    {:realworld.command/name :realworld.account/register
                     :realworld.command/parameters
                     {:realworld.account/email    "alice..smith@example-.com"
                      :realworld.account/password "short "}})]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type :validation
                     :realworld.error/messages
                     #{"Email is invalid"
                       "Password must be at least 8 characters"
                       "Password must not contain whitespace"}}}
                   (:realworld.application/response result)))
        (expect (nil? (::effect.create result)))))

    (it "rejects an email that is already registered"
      {:skip true}
      (expect false))))
