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

(def login-command
  {:realworld.command/name       :realworld.account/login
   :realworld.command/parameters {:realworld.account/email    "alice@example.com"
                                  :realworld.account/password "secret123"}})

(def register-command-schema
  (get-in account/command-definitions
          [:realworld.account/register :realworld.command/schema]))

(def login-command-schema
  (get-in account/command-definitions
          [:realworld.account/login :realworld.command/schema]))

(def authenticated-account
  {:realworld.account/id    "id"
   :realworld.account/email "alice@example.com"})

(defn password-hash [password]
  (str "hash:" password))

(defn created-account [{:realworld.account/keys [email password]}]
  {:realworld.account/id            "id"
   :realworld.account/email         email
   :realworld.account/password-hash (password-hash password)})

(def coeffect-resolvers
  {:realworld.account/authenticate (constantly authenticated-account)
   :realworld.account/by-email     (constantly nil)
   :realworld.password/hash        password-hash
   :realworld.uuid/generate        (constantly "id")})

(def effect-interpreters
  {:realworld.account/create
   (fn [context account]
     (assoc context ::effect.create account))
   :realworld.session/start
   (fn [context account]
     (assoc context ::effect.activate account))})

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
  "Commands spanning the valid and invalid regions of registration."
  (generators/let [command (schema/generator register-command-schema)
                   mutate (generators/elements (vals mutations))]
    (mutate command)))

(def login-mutations
  {:empty-password    #(assoc-in %
                                 [:realworld.command/parameters
                                  :realworld.account/password]
                                 "")
   :invalid-email     #(assoc-in %
                                 [:realworld.command/parameters
                                  :realworld.account/email]
                                 "alice..smith@example.com")
   :remove-email      #(update % :realworld.command/parameters
                               dissoc :realworld.account/email)
   :remove-password   #(update % :realworld.command/parameters
                               dissoc :realworld.account/password)
   :remove-parameters #(dissoc % :realworld.command/parameters)
   :unchanged         identity})

(def login-command-generator
  "Commands spanning the valid and invalid regions of login."
  (generators/let [command (schema/generator login-command-schema)
                   mutate (generators/elements (vals login-mutations))]
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
                    :realworld.response/effects [[:realworld.account/create
                                                  {:realworld.account/id            "id"
                                                   :realworld.account/email         "alice@example.com"
                                                   :realworld.account/password-hash "hash:secret123"}]]}
                   (:realworld.application/response result)))
        (expect (= {:realworld.account/id            "id"
                    :realworld.account/email         "alice@example.com"
                    :realworld.account/password-hash "hash:secret123"}
                   (::effect.create result)))))

    (it "registers every command allowed by its schema"
      (check!
       (properties/for-all [command (schema/generator register-command-schema)]
                           (let [result (application/dispatch (test-context) command)
                                 parameters (:realworld.command/parameters command)
                                 response (:realworld.application/response result)]
                             (= {:outcome     :ok
                                 :event-email (:realworld.account/email parameters)
                                 :created     (created-account parameters)}
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
                             (if (schema/valid? register-command-schema command)
                               (and (= :ok (:realworld.response/outcome response))
                                    (= (created-account
                                        (:realworld.command/parameters command))
                                       created))
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
      (let [looked-up-email (atom nil)
            context (test-context
                     {:coeffect-resolvers
                      {:realworld.account/by-email
                       (fn [email]
                         (reset! looked-up-email email)
                         {:realworld.account/id    "existing-id"
                          :realworld.account/email email})}})
            result (application/dispatch context registration-command)]
        (expect (= "alice@example.com" @looked-up-email))
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type     :domain
                     :realworld.error/messages #{"Email is already taken"}}}
                   (:realworld.application/response result)))
        (expect (nil? (::effect.create result))))))

  (describe "login"
    (it "logs in with valid credentials"
      (let [result (application/dispatch (test-context) login-command)]
        (expect (= {:realworld.response/outcome :ok
                    :realworld.response/data    {:realworld.account/id "id"}
                    :realworld.response/events  [{:realworld.event/type :realworld.account/logged-in
                                                  :realworld.account/id "id"}]
                    :realworld.response/effects [[:realworld.session/start
                                                  {:realworld.account/id "id"}]]}
                   (:realworld.application/response result)))
        (expect (= {:realworld.account/id "id"}
                   (::effect.activate result)))))

    (it "either logs in or rejects the command with validation errors"
      (check!
       (properties/for-all [command login-command-generator]
                           (let [valid? (schema/valid? login-command-schema command)
                                 result (application/dispatch (test-context) command)
                                 response (:realworld.application/response result)]
                             (= (if valid?
                                  {:outcome    :ok
                                   :error-type nil
                                   :messages?  false
                                   :activated  {:realworld.account/id "id"}}
                                  {:outcome    :error
                                   :error-type :validation
                                   :messages?  true
                                   :activated  nil})
                                {:outcome    (:realworld.response/outcome response)
                                 :error-type (get-in response
                                                     [:realworld.response/data
                                                      :realworld.error/type])
                                 :messages?  (boolean
                                              (seq
                                               (get-in response
                                                       [:realworld.response/data
                                                        :realworld.error/messages])))
                                 :activated  (::effect.activate result)})))))

    (it "rejects missing account and incorrect password identically"
      (doseq [[description parameters]
              [["missing account"
                {:realworld.account/email    "missing@example.com"
                 :realworld.account/password "secret123"}]
               ["incorrect password"
                {:realworld.account/email    "alice@example.com"
                 :realworld.account/password "incorrect"}]]]
        (let [result (application/dispatch
                      (test-context
                       {:coeffect-resolvers
                        {:realworld.account/authenticate (constantly nil)}})
                      {:realworld.command/name       :realworld.account/login
                       :realworld.command/parameters parameters})]
          (expect (= {:realworld.response/outcome :error
                      :realworld.response/data
                      {:realworld.error/type     :domain
                       :realworld.error/messages #{"Email or password is invalid"}}}
                     (:realworld.application/response result))
                  description)
          (expect (nil? (::effect.activate result)) description))))

    (it "validates credentials before authentication"
      (let [authentication-attempted? (atom false)
            result (application/dispatch
                    (test-context
                     {:coeffect-resolvers
                      {:realworld.account/authenticate
                       (fn [& _]
                         (reset! authentication-attempted? true))}})
                    {:realworld.command/name :realworld.account/login
                     :realworld.command/parameters
                     {:realworld.account/email "alice..smith@example.com"}})]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type :validation
                     :realworld.error/messages
                     #{"Email is invalid" "Password is required"}}}
                   (:realworld.application/response result)))
        (expect (false? @authentication-attempted?))
        (expect (nil? (::effect.activate result)))))

    (it "treats password strength as a credential concern"
      (let [authentication-parameters (atom nil)
            parameters {:realworld.account/email    "alice@example.com"
                        :realworld.account/password "short "}
            result (application/dispatch
                    (test-context
                     {:coeffect-resolvers
                      {:realworld.account/authenticate
                       (fn [email password]
                         (reset! authentication-parameters [email password])
                         nil)}})
                    {:realworld.command/name       :realworld.account/login
                     :realworld.command/parameters parameters})]
        (expect (= ["alice@example.com" "short "]
                   @authentication-parameters))
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type     :domain
                     :realworld.error/messages #{"Email or password is invalid"}}}
                   (:realworld.application/response result)))
        (expect (nil? (::effect.activate result)))))))
