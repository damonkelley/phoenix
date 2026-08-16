(ns realworld.application
  (:require [realworld.interceptor :as interceptor]
            [realworld.response :as response]
            [realworld.schema :as schema]))

(def resolve-command-definition
  {:name  :resolve-command-definition
   :enter (fn [context]
            (let [command-name (get-in context
                                       [:realworld.application/command
                                        :realworld.command/name])
                  definition (get-in context
                                     [:realworld.application/command-definitions
                                      command-name])]
              (if definition
                (assoc context
                       :realworld.application/command-definition
                       definition)
                (-> context
                    (assoc :realworld.application/response
                           (response/error
                            :data {:realworld.command/name command-name}))
                    (interceptor/terminate)))))})

(def validate-command
  {:name  :validate-command
   :enter (fn [context]
            (let [definition (get context :realworld.application/command-definition)
                  command-schema (:realworld.command/schema definition)
                  command (:realworld.application/command context)
                  errors (when command-schema
                           (schema/validate command-schema command))]
              (if (seq errors)
                (-> context
                    (assoc :realworld.application/response
                           (response/error
                            :data {:realworld.error/type     :validation
                                   :realworld.error/messages errors}))
                    (interceptor/terminate))
                context)))})

(def resolve-coeffects
  {:name  :resolve-coeffects
   :enter (fn [context]
            (let [declarations (get-in context
                                       [:realworld.application/command-definition
                                        :realworld.command/coeffects])
                  parameters (get-in context
                                     [:realworld.application/command
                                      :realworld.command/parameters])
                  resolvers (:realworld.application/coeffect-resolvers context)
                  coeffects (reduce-kv
                             (fn [resolved key [operation & parameter-paths]]
                               (assoc resolved
                                      key
                                      (apply (get resolvers operation)
                                             (map #(get-in parameters %)
                                                  parameter-paths))))
                             {}
                             declarations)]
              (assoc context
                     :realworld.application/coeffects
                     coeffects)))})

(def invoke-command-definition
  {:name  :invoke-command-definition
   :enter (fn [context]
            (let [handler (get-in context
                                  [:realworld.application/command-definition
                                   :realworld.command/handler])
                  coeffects (:realworld.application/coeffects context)
                  parameters (get-in context
                                     [:realworld.application/command
                                      :realworld.command/parameters])]
              (assoc context
                     :realworld.application/response
                     (handler coeffects parameters))))})

(def handle-operational-error
  {:name  :handle-operational-error
   :error (fn [context error]
            (-> context
                (assoc :realworld.application/error error)
                (assoc :realworld.application/response
                       (response/error
                        :data {:realworld.error/type :operational}))))})

(def interpret-effects
  {:name  :interpret-effects
   :leave (fn [context]
            (let [effects (get-in context
                                  [:realworld.application/response
                                   :realworld.response/effects])
                  interpreters (:realworld.application/effect-interpreters context)]
              (reduce
               (fn [context [operation & args]]
                 (apply (get interpreters operation) context args))
               context
               effects)))})

(def interceptor-queue
  [handle-operational-error
   interpret-effects
   resolve-command-definition
   validate-command
   resolve-coeffects
   invoke-command-definition])

(defn context [{:keys [command-definitions
                       coeffect-resolvers
                       effect-interpreters]}]
  (interceptor/enqueue
   {:realworld.application/command-definitions command-definitions
    :realworld.application/coeffect-resolvers  coeffect-resolvers
    :realworld.application/effect-interpreters effect-interpreters}
   interceptor-queue))

(defn dispatch [context command]
  (-> context
      (assoc :realworld.application/command command)
      (interceptor/execute)))
