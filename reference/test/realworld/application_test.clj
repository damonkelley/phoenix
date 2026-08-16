(ns realworld.application-test
  (:require [lazytest.core :refer [defdescribe describe expect it]]
            [realworld.application :as application]
            [realworld.command :as command]))

(def command
  {:realworld.command/name       :test.command/execute
   :realworld.command/parameters {:test/value "value"}})

(def Command
  (command/schema
   :test.command/execute
   [:map
    [:test/value :string]]))

(def command-definitions
  {:test.command/execute
   {:realworld.command/schema    Command
    :realworld.command/coeffects {:test/id [:test/generate-id]}
    :realworld.command/handler   (fn [{:test/keys [id]}
                                      {:test/keys [value]}]
                                   {:realworld.response/outcome :ok
                                    :realworld.response/data    {:test/id id}
                                    :realworld.response/effects [[:test/perform
                                                                  {:test/id    id
                                                                   :test/value value}]]})}
   :test.command/describe
   {:realworld.command/coeffects {}
    :realworld.command/handler   (fn [_ _]
                                   {:realworld.response/outcome :ok
                                    :realworld.response/data    "description"})}
   :test.command/lookup
   {:realworld.command/coeffects
    {:test/found [:test/find [:test/value]]}
    :realworld.command/handler
    (fn [{:test/keys [found]} _]
      {:realworld.response/outcome :ok
       :realworld.response/data    found})}})

(defn unexpected [operation]
  (fn [& _]
    (throw (ex-info "Unexpected operation" {:operation operation}))))

(def coeffect-resolvers
  {:test/find        identity
   :test/generate-id (fn [] "id")})

(def effect-interpreters
  {:test/perform (fn [context _parameters] context)})

(defn test-context
  ([]
   (test-context {}))
  ([options]
   (application/context
     {:command-definitions command-definitions
      :coeffect-resolvers
      (merge coeffect-resolvers (:coeffect-resolvers options))
      :effect-interpreters
      (merge effect-interpreters (:effect-interpreters options))})))

(defdescribe application
  (describe "command dispatch"
    (it "invokes a known command"
      (let [context (test-context
                     {:effect-interpreters
                      {:test/perform
                       (fn [context parameters]
                         (assoc context :test/interpreted parameters))}})
            result (application/dispatch context command)]
        (expect (= {:realworld.response/outcome :ok
                    :realworld.response/data    {:test/id "id"}
                    :realworld.response/effects [[:test/perform
                                                  {:test/id    "id"
                                                   :test/value "value"}]]}
                   (:realworld.application/response result)))
        (expect (= {:test/id    "id"
                    :test/value "value"}
                   (:test/interpreted result)))))

    (it "returns an error for an unknown command without running coeffects or effects"
      (let [context (test-context
                     {:coeffect-resolvers
                      {:test/generate-id (unexpected :coeffect)}
                      :effect-interpreters
                      {:test/perform (unexpected :effect)}})
            result (application/dispatch
                    context
                    {:realworld.command/name :test.command/unknown})]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.command/name :test.command/unknown}}
                   (:realworld.application/response result)))
        (expect (nil? (:realworld.application/error result)))))

    (it "allows a command without coeffects or effects"
      (let [result (application/dispatch
                    (test-context)
                    {:realworld.command/name :test.command/describe})]
        (expect (= {:realworld.response/outcome :ok
                    :realworld.response/data    "description"}
                   (:realworld.application/response result))))))

  (describe "command validation"
    (it "returns validation errors before resolving coeffects or interpreting effects"
      (let [context (test-context
                     {:coeffect-resolvers
                      {:test/generate-id (unexpected :coeffect)}
                      :effect-interpreters
                      {:test/perform (unexpected :effect)}})
            result (application/dispatch
                    context
                    {:realworld.command/name       :test.command/execute
                     :realworld.command/parameters {}})]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type     :validation
                     :realworld.error/messages #{"Value is required"}}}
                   (:realworld.application/response result)))
        (expect (nil? (:realworld.application/error result))))))

  (describe "coeffect resolution"
    (it "resolves coeffect arguments from parameter paths"
      (let [result (application/dispatch
                    (test-context)
                    {:realworld.command/name       :test.command/lookup
                     :realworld.command/parameters {:test/value "value"}})]
        (expect (= {:realworld.response/outcome :ok
                    :realworld.response/data    "value"}
                   (:realworld.application/response result)))))

    (it "turns resolver failures into operational errors"
      (let [failure (ex-info "Coeffect unavailable" {})
            context (test-context
                     {:coeffect-resolvers
                      {:test/generate-id
                       (fn []
                         (throw failure))}})
            result (application/dispatch context command)]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type :operational}}
                   (:realworld.application/response result)))
        (expect (identical? failure
                            (:realworld.application/error result))))))

  (describe "effect interpretation"
    (it "turns interpreter failures into operational errors"
      (let [failure (ex-info "Effect unavailable" {})
            context (test-context
                     {:effect-interpreters
                      {:test/perform
                       (fn [_context _parameters]
                         (throw failure))}})
            result (application/dispatch context command)]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type :operational}}
                   (:realworld.application/response result)))
        (expect (identical? failure
                            (:realworld.application/error result)))))))
