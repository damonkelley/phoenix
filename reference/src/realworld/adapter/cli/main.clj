(ns realworld.adapter.cli.main
  (:require [babashka.cli :as cli]
            [clojure.string :as string]
            [realworld.account :as account]
            [realworld.adapter.hashing :as hashing]
            [realworld.adapter.sqlite :as sqlite]
            [realworld.application :as application]
            [realworld.interceptor :as interceptor])
  (:gen-class))

(def default-database-path "realworld.db")

(defn- authenticate [database email password]
  (when-let [account (sqlite/account-by-email database email)]
    (when (hashing/matches? password
                            (:realworld.account/password-hash account))
      (select-keys account
                   [:realworld.account/id
                    :realworld.account/email]))))

(defn- application-context [database]
  (application/context
    {:command-definitions account/command-definitions
     :coeffect-resolvers  {:realworld.account/authenticate (fn [email password]
                                                             (authenticate database email password))
                           :realworld.account/by-email     (fn [email]
                                                             (sqlite/account-by-email database email))
                           :realworld.password/hash        hashing/hash-password
                           :realworld.uuid/generate        random-uuid}
     :effect-interpreters {:realworld.account/create (fn [context account]
                                                       (sqlite/create-account! database account)
                                                       context)
                           :realworld.session/start  (fn [context account]
                                                       (sqlite/activate-account! database account)
                                                       context)}}))

(defn initialize [database-path]
  (let [database (sqlite/database database-path)]
    (sqlite/initialize! database)
    (application-context database)))

(defn dispatch [context command]
  (application/dispatch context command))

(defn- account-command [command-name opts]
  {:realworld.command/name command-name
   :realworld.command/parameters
   (update-keys opts #(keyword "realworld.account" (name %)))})

(defn- register [{:keys [opts]}]
  (account-command :realworld.account/register opts))

(defn- login [{:keys [opts]}]
  (account-command :realworld.account/login opts))

(def ^:private credential-options
  {:email    {:coerce :string
              :desc   "Account email address"}
   :password {:coerce :string
              :desc   "Account password"}})

(def command-table
  [{:cmds     ["register"]
    :fn       register
    :spec     credential-options
    :restrict true}
   {:cmds     ["login"]
    :fn       login
    :spec     credential-options
    :restrict true}])

(defn- error-message [response]
  (let [data (:realworld.response/data response)
        messages (:realworld.error/messages data)]
    (if (seq messages)
      (string/join (System/lineSeparator) (sort messages))
      (case (:realworld.error/type data)
        :operational "Operation failed"
        "Command failed"))))

(defn- command-result [context]
  (let [response (:realworld.application/response context)]
    (if (= :ok (:realworld.response/outcome response))
      {:exit   0
       :output "Success"
       :result response}
      {:exit   1
       :error  (error-message response)
       :result response})))

(defn- operational-result [error]
  {:exit  1
   :error "Operation failed"
   :cause error})

(def handle-error
  {:name  ::handle-error
   :error (fn [context error]
            (assoc context
                   ::result
                   (if (and (instance? clojure.lang.ExceptionInfo error)
                            (= :org.babashka/cli (:type (ex-data error))))
                     {:exit  2
                      :error (ex-message error)}
                     (operational-result error))))})

(def parse-arguments
  {:name  ::parse-arguments
   :enter (fn [context]
            (assoc context
                   ::command
                   (cli/dispatch command-table (::arguments context))))})

(def initialize-application
  {:name  ::initialize-application
   :enter (fn [context]
            (assoc context
                   ::application-context
                   ((::application-factory context)
                    (::database-path context))))})

(def invoke-command
  {:name  ::invoke-command
   :enter (fn [context]
            (assoc context
                   ::application-result
                   ((::dispatch-command context)
                    (::application-context context)
                    (::command context))))})

(def format-result
  {:name  ::format-result
   :enter (fn [context]
            (assoc context
                   ::result
                   (command-result (::application-result context))))})

(def cli-interceptors
  [handle-error
   parse-arguments
   initialize-application
   invoke-command
   format-result])

(defn run
  ([arguments]
   (run arguments {}))
  ([arguments {:keys [application-factory database-path dispatch-command]
               :or   {application-factory initialize
                      database-path       default-database-path
                      dispatch-command    dispatch}}]
   (-> {::arguments           arguments
        ::application-factory application-factory
        ::database-path       database-path
        ::dispatch-command    dispatch-command}
       (interceptor/execute cli-interceptors)
       (::result))))

(defn -main [& arguments]
  (let [{:keys [exit output error]} (run arguments)]
    (when output
      (println output))
    (when error
      (binding [*out* *err*]
        (println error)))
    (shutdown-agents)
    (System/exit exit)))
