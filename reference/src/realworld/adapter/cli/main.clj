(ns realworld.adapter.cli.main
  (:require [babashka.cli :as cli])
  (:gen-class))

(def register-spec
  {:email {:desc "Account email address"}
   :password {:desc "Account password"}})

(defn- register [{:keys [opts]}]
  ;; The walking skeleton stops at the application boundary. Registration
  ;; behavior will be connected here in the next vertical step.
  {:command :register
   :options (select-keys opts [:email :password])})

(def command-table
  [{:cmds ["register"]
    :fn register
    :spec register-spec
    :restrict true}])

(defn run [arguments]
  (try
    {:exit 0
     :result (cli/dispatch command-table arguments)}
    (catch clojure.lang.ExceptionInfo exception
      {:exit 2
       :error (ex-message exception)})))

(defn -main [& arguments]
  (let [{:keys [exit error]} (run arguments)]
    (when error
      (binding [*out* *err*]
        (println error)))
    (shutdown-agents)
    (System/exit exit)))
