(ns realworld.article-test
  (:require [clojure.test.check :as test.check]
            [clojure.test.check.generators :as generators]
            [clojure.test.check.properties :as properties]
            [lazytest.core :refer [defdescribe describe expect it]]
            [realworld.application :as application]
            [realworld.article :as article]
            [realworld.schema :as schema]))

(def creation-command
  {:realworld.command/name :realworld.article/create
   :realworld.command/parameters
   {:realworld.article/title       "Hello World"
    :realworld.article/description "An introduction"
    :realworld.article/body        "Article contents"
    :realworld.article/tags        ["clojure" "sqlite"]}})

(def feed-command
  {:realworld.command/name       :realworld.article/feed
   :realworld.command/parameters {}})

(def create-command-schema
  (get-in article/command-definitions
          [:realworld.article/create :realworld.command/schema]))

(def authenticated-account
  {:realworld.account/id    "account-id"
   :realworld.account/email "alice@example.com"})

(def now
  (java.time.Instant/parse "2026-08-16T12:00:00Z"))

(def slug
  "hello-world-000000")

(def feed-articles
  [{:realworld.article/title  "Hello World"
    :realworld.article/slug   slug
    :realworld.article/tags   ["clojure" "sqlite"]
    :realworld.article/author {:realworld.account/email
                               "alice@example.com"}}])

(def coeffect-resolvers
  {:realworld.article/feed            (constantly feed-articles)
   :realworld.slug/generate           (constantly slug)
   :realworld.session/current-account (constantly authenticated-account)
   :realworld.time/now                (constantly now)})

(def effect-interpreters
  {:realworld.article/create
   (fn [context article]
     (assoc context ::effect.create article))})

(defn test-context
  ([]
   (test-context {}))
  ([options]
   (application/context
     {:command-definitions article/command-definitions
      :coeffect-resolvers
      (merge coeffect-resolvers (:coeffect-resolvers options))
      :effect-interpreters
      (merge effect-interpreters (:effect-interpreters options))})))

(defn check! [property]
  (let [result (test.check/quick-check 100 property)]
    (expect (:pass? result) (pr-str result))))

(def mutations
  {:blank-body         #(assoc-in %
                                  [:realworld.command/parameters
                                   :realworld.article/body]
                                  " \t")
   :blank-description  #(assoc-in %
                                  [:realworld.command/parameters
                                   :realworld.article/description]
                                  "")
   :blank-tag          #(assoc-in %
                                  [:realworld.command/parameters
                                   :realworld.article/tags]
                                  [""])
   :blank-title        #(assoc-in %
                                  [:realworld.command/parameters
                                   :realworld.article/title]
                                  " ")
   :remove-body        #(update % :realworld.command/parameters
                                dissoc :realworld.article/body)
   :remove-description #(update % :realworld.command/parameters
                                dissoc :realworld.article/description)
   :remove-parameters  #(dissoc % :realworld.command/parameters)
   :remove-title       #(update % :realworld.command/parameters
                                dissoc :realworld.article/title)
   :unchanged          identity})

(def create-command-generator
  "Commands spanning the valid and invalid regions of article creation."
  (generators/let [command (schema/generator create-command-schema)
                   mutate (generators/elements (vals mutations))]
    (mutate command)))

(defdescribe articles
  (describe "creation"
    (it "creates an article for the authenticated account"
      (let [result (application/dispatch (test-context) creation-command)
            created {:realworld.article/slug        slug
                     :realworld.article/title       "Hello World"
                     :realworld.article/description "An introduction"
                     :realworld.article/body        "Article contents"
                     :realworld.article/tags        ["clojure" "sqlite"]
                     :realworld.article/author-id   "account-id"
                     :realworld.article/created-at  now
                     :realworld.article/updated-at  now}]
        (expect (= {:realworld.response/outcome :ok
                    :realworld.response/data
                    {:realworld.article/slug slug}
                    :realworld.response/events
                    [(assoc created
                            :realworld.event/type
                            :realworld.article/created)]
                    :realworld.response/effects
                    [[:realworld.article/create created]]}
                   (:realworld.application/response result)))
        (expect (= created (::effect.create result)))))

    (it "requires an authenticated account"
      (let [result (application/dispatch
                    (test-context
                     {:coeffect-resolvers
                      {:realworld.session/current-account (constantly nil)}})
                    creation-command)]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type     :domain
                     :realworld.error/messages #{"Login is required"}}}
                   (:realworld.application/response result)))
        (expect (nil? (::effect.create result)))))

    (it "reports all applicable validation errors before resolving coeffects"
      (let [coeffect-resolved? (atom false)
            unexpected (fn [& _]
                         (reset! coeffect-resolved? true))
            result (application/dispatch
                    (test-context
                     {:coeffect-resolvers
                      {:realworld.slug/generate           unexpected
                       :realworld.session/current-account unexpected
                       :realworld.time/now                unexpected}})
                    {:realworld.command/name :realworld.article/create
                     :realworld.command/parameters
                     {:realworld.article/title       " "
                      :realworld.article/description ""
                      :realworld.article/body        "\t"
                      :realworld.article/tags        ["clojure" ""]}})]
        (expect (= {:realworld.response/outcome :error
                    :realworld.response/data
                    {:realworld.error/type :validation
                     :realworld.error/messages
                     #{"Title is required"
                       "Description is required"
                       "Body is required"
                       "Tag must not be blank"}}}
                   (:realworld.application/response result)))
        (expect (false? @coeffect-resolved?))
        (expect (nil? (::effect.create result)))))

    (it "either creates an article or rejects the command with validation errors"
      (check!
       (properties/for-all [command create-command-generator]
                           (let [valid? (schema/valid? create-command-schema command)
                                 result (application/dispatch (test-context) command)
                                 response (:realworld.application/response result)]
                             (= (if valid?
                                  {:outcome    :ok
                                   :error-type nil
                                   :messages?  false
                                   :created?   true}
                                  {:outcome    :error
                                   :error-type :validation
                                   :messages?  true
                                   :created?   false})
                                {:outcome    (:realworld.response/outcome response)
                                 :error-type (get-in response
                                                     [:realworld.response/data
                                                      :realworld.error/type])
                                 :messages?  (boolean
                                              (seq
                                               (get-in response
                                                       [:realworld.response/data
                                                        :realworld.error/messages])))
                                 :created?   (some? (::effect.create result))}))))))

  (describe "feed"
    (it "returns the resolved global article feed without authentication"
      (let [result (application/dispatch
                    (test-context
                     {:coeffect-resolvers
                      {:realworld.session/current-account
                       (fn []
                         (throw (ex-info "Unexpected authentication" {})))}})
                    feed-command)]
        (expect (= {:realworld.response/outcome :ok
                    :realworld.response/data
                    {:realworld.article/articles feed-articles}}
                   (:realworld.application/response result)))))

    (it "returns an empty feed"
      (let [result (application/dispatch
                    (test-context
                     {:coeffect-resolvers
                      {:realworld.article/feed (constantly [])}})
                    feed-command)]
        (expect (= {:realworld.response/outcome :ok
                    :realworld.response/data
                    {:realworld.article/articles []}}
                   (:realworld.application/response result)))))))
