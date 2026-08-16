(ns realworld.application
  (:require [realworld.interceptor :as interceptor]
            [realworld.response :as response]
            [realworld.command :as command]
            [realworld.schema :as schema]))

(def resolve-command-definition
  {:name  ::resolve-command-definition
   :enter (fn [context]
            (let [command-name (get-in context
                                       [::command
                                        ::command/name])
                  definition (get-in context
                                     [::command-definitions
                                      command-name])]
              (if definition
                (assoc context
                       ::command-definition
                       definition)
                (-> context
                    (assoc ::response
                           (response/error
                            :data {:realworld.command/name command-name}))
                    (interceptor/terminate)))))})

(def validate-command
  {:name  ::validate-command
   :enter (fn [context]
            (let [definition (get context ::command-definition)
                  command-schema (:realworld.command/schema definition)
                  command (::command context)
                  errors (when command-schema
                           (schema/validate command-schema command))]
              (if (seq errors)
                (-> context
                    (assoc ::response
                           (response/error
                            :data {:realworld.error/type     :validation
                                   :realworld.error/messages errors}))
                    (interceptor/terminate))
                context)))})

(def resolve-coeffects
  {:name  ::resolve-coeffects
   :enter (fn [context]
            (let [declarations (get-in context
                                       [::command-definition
                                        ::command/coeffects])
                  parameters (get-in context
                                     [::command
                                      ::command/parameters])
                  resolvers (::coeffect-resolvers context)
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
                     ::coeffects
                     coeffects)))})

(def invoke-command-definition
  {:name  :invoke-command-definition
   :enter (fn [context]
            (let [handler (get-in context
                                  [::command-definition
                                   ::command/handler])
                  coeffects (::coeffects context)
                  parameters (get-in context
                                     [::command
                                      ::command/parameters])]
              (assoc context
                     ::response
                     (handler coeffects parameters))))})

(def handle-operational-error
  {:name  :handle-operational-error
   :error (fn [context error]
            (-> context
                (assoc ::error error)
                (assoc ::response
                       (response/error
                        :data {:realworld.error/type :operational}))))})

(def interpret-effects
  {:name  :interpret-effects
   :leave (fn [context]
            (let [effects (get-in context
                                  [::response
                                   ::response/effects])
                  interpreters (::effect-interpreters context)]
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
   {::command-definitions command-definitions
    ::coeffect-resolvers  coeffect-resolvers
    ::effect-interpreters effect-interpreters}
   interceptor-queue))

(defn dispatch [context command]
  (-> context
      (assoc ::command command)
      (interceptor/execute)))
