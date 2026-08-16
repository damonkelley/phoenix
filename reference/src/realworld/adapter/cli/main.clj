(ns realworld.adapter.cli.main
  (:require [babashka.cli :as cli]
            [clojure.string :as string]
            [realworld.account :as account]
            [realworld.adapter.authentication :as authentication]
            [realworld.adapter.hashing :as hashing]
            [realworld.adapter.slug :as slug]
            [realworld.adapter.sqlite :as sqlite]
            [realworld.application :as application]
            [realworld.article :as article]
            [realworld.interceptor :as interceptor])
  (:gen-class))

(def default-database-path "realworld.db")

(defn- application-context [database]
  (application/context
    {:command-definitions (merge account/command-definitions
                                 article/command-definitions)
     :coeffect-resolvers  {:realworld.account/by-credentials  (fn [email password]
                                                                (authentication/account-by-credentials database email password))
                           :realworld.account/by-email        (fn [email]
                                                                (sqlite/account-by-email database email))
                           :realworld.article/feed            (fn []
                                                                (sqlite/article-feed database))
                           :realworld.slug/generate           (fn [title]
                                                                (slug/from-title title
                                                                                 (random-uuid)))
                           :realworld.password/hash           hashing/hash-password
                           :realworld.session/current-account (fn []
                                                                (when-let [account-id (sqlite/session-account-id database)]
                                                                  {:realworld.account/id account-id}))
                           :realworld.time/now                (fn []
                                                                (java.time.Instant/now))
                           :realworld.uuid/generate           random-uuid}
     :effect-interpreters {:realworld.account/create (fn [context account]
                                                       (sqlite/create-account! database account)
                                                       context)
                           :realworld.article/create (fn [context article]
                                                       (sqlite/create-article! database article)
                                                       context)
                           :realworld.session/start  (fn [context account]
                                                       (sqlite/start-session! database account)
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

(defn- create-article [{:keys [opts]}]
  (let [{:keys [title description body tag]} opts]
    {:realworld.command/name :realworld.article/create
     :realworld.command/parameters
     {:realworld.article/title       title
      :realworld.article/description description
      :realworld.article/body        body
      :realworld.article/tags        (vec (or tag []))}}))

(defn- article-feed [_arguments]
  {:realworld.command/name       :realworld.article/feed
   :realworld.command/parameters {}})

(def ^:private credential-options
  {:email    {:coerce :string
              :desc   "Account email address"}
   :password {:coerce :string
              :desc   "Account password"}})

(def command-table
  [{:cmds     ["account" "register"]
    :fn       register
    :spec     credential-options
    :restrict true}
   {:cmds     ["account" "login"]
    :fn       login
    :spec     credential-options
    :restrict true}
   {:cmds     ["article" "create"]
    :fn       create-article
    :spec     {:title       {:coerce :string
                             :desc   "Article title"}
               :description {:coerce :string
                             :desc   "Article description"}
               :body        {:coerce :string
                             :desc   "Article body"}
               :tag         {:coerce [:string]
                             :desc   "Article tag"}}
    :restrict true}
   {:cmds     ["article" "feed"]
    :fn       article-feed
    :spec     {}
    :restrict true}])

(defn- error-message [response]
  (let [data (:realworld.response/data response)
        messages (:realworld.error/messages data)]
    (if (seq messages)
      (string/join (System/lineSeparator) (sort messages))
      (case (:realworld.error/type data)
        :operational "Operation failed"
        "Command failed"))))

(def ^:private article-feed-headings
  ["TITLE" "SLUG" "TAGS" "AUTHOR"])

(defn- article-feed-row [article]
  (let [tags (:realworld.article/tags article)]
    [(:realworld.article/title article)
     (:realworld.article/slug article)
     (if (seq tags)
       (string/join ", " tags)
       "(none)")
     (get-in article
             [:realworld.article/author
              :realworld.account/email])]))

(defn- column-widths [rows]
  (mapv (fn [column]
          (apply max (map count column)))
        (apply map vector rows)))

(defn- pad-right [value width]
  (str value (apply str (repeat (- width (count value)) " "))))

(defn- table-row [widths cells]
  (string/join
   "  "
   (map-indexed (fn [index value]
                  (if (= index (dec (count cells)))
                    value
                    (pad-right value (nth widths index))))
                cells)))

(defn- article-feed-output [articles]
  (if (seq articles)
    (let [article-rows (mapv article-feed-row articles)
          rows (into [article-feed-headings] article-rows)
          widths (column-widths rows)
          separator (mapv #(apply str (repeat % "-")) widths)]
      (string/join (System/lineSeparator)
                   (map #(table-row widths %)
                        (into [article-feed-headings separator]
                              article-rows))))
    "No articles"))

(defn- success-output [response]
  (let [data (:realworld.response/data response)]
    (cond
      (contains? data :realworld.article/articles)
      (article-feed-output (:realworld.article/articles data))

      (:realworld.article/slug data)
      (:realworld.article/slug data)

      :else
      "Success")))

(defn- command-result [context]
  (let [response (:realworld.application/response context)]
    (if (= :ok (:realworld.response/outcome response))
      {:exit   0
       :output (success-output response)
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
