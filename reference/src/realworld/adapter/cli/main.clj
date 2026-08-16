(ns realworld.adapter.cli.main
  (:require [babashka.cli :as cli]
            [clojure.string :as string]
            [realworld.account :as account]
            [realworld.adapter.hashing :as hashing]
            [realworld.adapter.sqlite :as sqlite]
            [realworld.application :as application])
  (:gen-class))

(def default-database-path "realworld.db")

(defn- application-context [database]
  (application/context
    {:command-definitions account/command-definitions
     :coeffect-resolvers  {:realworld.account/by-email (fn [email]
                                                         (sqlite/account-by-email database email))
                           :realworld.password/hash    hashing/hash-password
                           :realworld.uuid/generate    random-uuid}
     :effect-interpreters {:realworld.account/create (fn [context account]
                                                       (sqlite/create-account! database account)
                                                       context)}}))

(defn initialize [database-path]
  (let [database (sqlite/database database-path)]
    (sqlite/initialize! database)
    (application-context database)))

(defn dispatch [context command]
  (application/dispatch context command))

(defn- register [{:keys [opts]}]
  {:realworld.command/name :realworld.account/register
   :realworld.command/parameters
   (update-keys opts #(keyword "realworld.account" (name %)))})

(def command-table
  [{:cmds     ["register"]
    :fn       register
    :spec     {:email    {:coerce :string
                          :desc   "Account email address"}
               :password {:coerce :string
                          :desc   "Account password"}}
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

(defn run
  ([arguments]
   (run arguments
        (fn [command]
          (dispatch (initialize default-database-path) command))))
  ([arguments dispatch-command]
   (try
     (-> (cli/dispatch command-table arguments)
         (dispatch-command)
         (command-result))
     (catch clojure.lang.ExceptionInfo exception
       (if (= :org.babashka/cli (:type (ex-data exception)))
         {:exit  2
          :error (ex-message exception)}
         (operational-result exception)))
     (catch Throwable error
       (operational-result error)))))

(defn -main [& arguments]
  (let [{:keys [exit output error]} (run arguments)]
    (when output
      (println output))
    (when error
      (binding [*out* *err*]
        (println error)))
    (shutdown-agents)
    (System/exit exit)))
