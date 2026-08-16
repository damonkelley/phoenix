(ns realworld.article
  (:require [clojure.string :as string]
            [realworld.command :as command]
            [realworld.response :as response]))

(defn- present? [value]
  (or (not (string? value))
      (not (string/blank? value))))

(defn- required-text [message]
  [:and {:gen/schema [:string {:min 1}]}
   [:string {:error/message message}]
   [:fn {:error/message message} present?]])

(def Title
  (required-text "Title is required"))

(def Description
  (required-text "Description is required"))

(def Body
  (required-text "Body is required"))

(def Tag
  (required-text "Tag must not be blank"))

(def CreateParameters
  [:map
   [:realworld.article/title Title]
   [:realworld.article/description Description]
   [:realworld.article/body Body]
   [:realworld.article/tags {:optional true} [:vector Tag]]])

(def FeedParameters
  [:map])

(defn- create [{:realworld.account/keys [authenticated-account]
                :realworld.article/keys [slug]
                :realworld.time/keys    [now]}
               {:realworld.article/keys [title description body tags]}]
  (if-not authenticated-account
    (response/error
     :data {:realworld.error/type     :domain
            :realworld.error/messages #{"Login is required"}})
    (let [article {:realworld.article/slug        slug
                   :realworld.article/title       title
                   :realworld.article/description description
                   :realworld.article/body        body
                   :realworld.article/tags        (vec (or tags []))
                   :realworld.article/author-id   (:realworld.account/id authenticated-account)
                   :realworld.article/created-at  now
                   :realworld.article/updated-at  now}]
      (response/ok
       :data {:realworld.article/slug slug}
       :events [(assoc article
                       :realworld.event/type
                       :realworld.article/created)]
       :effects [[:realworld.article/create article]]))))

(defn- feed [{:realworld.article/keys [articles]} _parameters]
  (response/ok
   :data {:realworld.article/articles articles}))

(def command-definitions
  {:realworld.article/create
   {:realworld.command/schema    (command/schema :realworld.article/create CreateParameters)
    :realworld.command/coeffects {:realworld.account/authenticated-account
                                  [:realworld.session/current-account]
                                  :realworld.article/slug
                                  [:realworld.slug/generate
                                   [:realworld.article/title]]
                                  :realworld.time/now
                                  [:realworld.time/now]}
    :realworld.command/handler   create}

   :realworld.article/feed
   {:realworld.command/schema    (command/schema :realworld.article/feed FeedParameters)
    :realworld.command/coeffects {:realworld.article/articles
                                  [:realworld.article/feed]}
    :realworld.command/handler   feed}})
