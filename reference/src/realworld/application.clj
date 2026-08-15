(ns realworld.application
  (:require [exoscale.interceptor :as interceptor]))

(def command-definitions
  {:realworld.system/describe
   {:realworld.command/coeffects {}
    :realworld.command/handler   (fn [_ _]
                                   {:realworld.response/outcome :ok
                                    :realworld.response/data    "Hello, world"})}
   :realworld.account/register
   {:realworld.command/coeffects {:realworld.account/id [:realworld.uuid/generate]}
    :realworld.command/handler   (fn [{:realworld.account/keys [id]}
                                      {:realworld.account/keys [email password]}]
                                   {:realworld.response/outcome :ok
                                    :realworld.response/data    {:realworld.account/id id}
                                    :realworld.response/events  [{:realworld.event/type    :realworld.account/registered
                                                                  :realworld.account/id    id
                                                                  :realworld.account/email email}]
                                    :realworld.response/effects [[:realworld.repository/create
                                                                  {:realworld.account/email    email
                                                                   :realworld.account/password password}]]})}})

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
                           {:realworld.response/outcome :error
                            :realworld.response/data    {:realworld.command/name command-name}})
                    (interceptor/terminate)))))})

(def resolve-coeffects
  {:name  :resolve-coeffects
   :enter (fn [context]
            (let [declarations (get-in context
                                       [:realworld.application/command-definition
                                        :realworld.command/coeffects])
                  resolvers (:realworld.application/coeffect-resolvers context)
                  coeffects (reduce-kv
                             (fn [resolved key [operation & args]]
                               (assoc resolved
                                      key
                                      (apply (get resolvers operation) args)))
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
                       {:realworld.response/outcome :error
                        :realworld.response/data
                        {:realworld.error/type :operational}})))})

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
