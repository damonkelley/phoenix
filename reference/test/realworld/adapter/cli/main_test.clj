(ns realworld.adapter.cli.main-test
  (:require [clojure.string :as string]
            [lazytest.core :refer [defdescribe describe expect it]]
            [realworld.adapter.cli.main :as cli]
            [realworld.response :as response]))

(defn unexpected-dispatch [& _]
  (throw (ex-info "Unexpected dispatch" {})))

(defdescribe command-line-interface
  (describe "account register command"
    (it "dispatches registration arguments"
      (let [application-context (Object.)
            initialized-path (atom nil)
            dispatched (atom nil)
            result (cli/run
                    ["account" "register"
                     "--email" "alice@example.com"
                     "--password" "12345678"]
                    {:application-factory
                     (fn [database-path]
                       (reset! initialized-path database-path)
                       application-context)
                     :dispatch-command
                     (fn [context command]
                       (reset! dispatched [context command])
                       {:realworld.application/response
                        (response/ok)})})]
        (expect (= cli/default-database-path @initialized-path))
        (expect (identical? application-context (first @dispatched)))
        (expect (= {:realworld.command/name :realworld.account/register
                    :realworld.command/parameters
                    {:realworld.account/email    "alice@example.com"
                     :realworld.account/password "12345678"}}
                   (second @dispatched)))
        (expect (= 0 (:exit result)))
        (expect (= "Success" (:output result)))))

    (it "rejects unknown options"
      (let [result (cli/run ["account" "register" "--unknown" "value"]
                            {:application-factory unexpected-dispatch
                             :dispatch-command    unexpected-dispatch})]
        (expect (= 2 (:exit result)))
        (expect (string? (:error result))))))

  (describe "account login command"
    (it "dispatches login arguments"
      (let [application-context (Object.)
            dispatched (atom nil)
            result (cli/run
                    ["account" "login"
                     "--email" "alice@example.com"
                     "--password" "secret123"]
                    {:application-factory (constantly application-context)
                     :dispatch-command
                     (fn [context command]
                       (reset! dispatched [context command])
                       {:realworld.application/response
                        (response/ok)})})]
        (expect (identical? application-context (first @dispatched)))
        (expect (= {:realworld.command/name :realworld.account/login
                    :realworld.command/parameters
                    {:realworld.account/email    "alice@example.com"
                     :realworld.account/password "secret123"}}
                   (second @dispatched)))
        (expect (= 0 (:exit result)))
        (expect (= "Success" (:output result))))))

  (describe "article create command"
    (it "dispatches article content and repeated tags"
      (let [dispatched (atom nil)
            result (cli/run
                    ["article" "create"
                     "--title" "Hello World"
                     "--description" "An introduction"
                     "--body" "Article contents"
                     "--tag" "clojure"
                     "--tag" "sqlite"]
                    {:application-factory (constantly (Object.))
                     :dispatch-command
                     (fn [_context command]
                       (reset! dispatched command)
                       {:realworld.application/response
                        (response/ok
                         :data {:realworld.article/slug "hello-world"})})})]
        (expect (= {:realworld.command/name :realworld.article/create
                    :realworld.command/parameters
                    {:realworld.article/title       "Hello World"
                     :realworld.article/description "An introduction"
                     :realworld.article/body        "Article contents"
                     :realworld.article/tags        ["clojure" "sqlite"]}}
                   @dispatched))
        (expect (= 0 (:exit result)))
        (expect (= "hello-world" (:output result))))))

  (describe "article feed command"
    (it "dispatches the feed command and formats article summaries"
      (let [dispatched (atom nil)
            articles [{:realworld.article/title "Newest"
                       :realworld.article/slug  "newest"
                       :realworld.article/tags  []
                       :realworld.article/author
                       {:realworld.account/email "alice@example.com"}}
                      {:realworld.article/title "Older"
                       :realworld.article/slug  "older"
                       :realworld.article/tags  ["clojure" "sqlite"]
                       :realworld.article/author
                       {:realworld.account/email "bob@example.com"}}]
            result (cli/run
                    ["article" "feed"]
                    {:application-factory (constantly (Object.))
                     :dispatch-command
                     (fn [_context command]
                       (reset! dispatched command)
                       {:realworld.application/response
                        (response/ok
                         :data {:realworld.article/articles articles})})})]
        (expect (= {:realworld.command/name       :realworld.article/feed
                    :realworld.command/parameters {}}
                   @dispatched))
        (expect (= 0 (:exit result)))
        (expect (= (string/join
                    (System/lineSeparator)
                    ["TITLE   SLUG    TAGS             AUTHOR"
                     "------  ------  ---------------  -----------------"
                     "Newest  newest  (none)           alice@example.com"
                     "Older   older   clojure, sqlite  bob@example.com"])
                   (:output result)))))

    (it "formats an empty feed"
      (let [result (cli/run
                    ["article" "feed"]
                    {:application-factory (constantly (Object.))
                     :dispatch-command
                     (fn [_context _command]
                       {:realworld.application/response
                        (response/ok
                         :data {:realworld.article/articles []})})})]
        (expect (= 0 (:exit result)))
        (expect (= "No articles" (:output result))))))

  (describe "unknown command"
    (it "is rejected as malformed usage before initialization"
      (let [initialized? (atom false)
            result (with-redefs [cli/initialize
                                 (fn [_]
                                   (reset! initialized? true))]
                     (cli/run ["unknown"]))]
        (expect (= 2 (:exit result)))
        (expect (string? (:error result)))
        (expect (false? @initialized?)))))

  (describe "operational failure"
    (it "reports an unexpected dispatch failure"
      (let [failure (RuntimeException. "Unavailable")
            result (cli/run ["account" "register"
                             "--email" "alice@example.com"
                             "--password" "12345678"]
                            {:application-factory (constantly (Object.))
                             :dispatch-command
                             (fn [_context _command]
                               (throw failure))})]
        (expect (= 1 (:exit result)))
        (expect (= "Operation failed" (:error result)))
        (expect (identical? failure (:cause result)))))))
