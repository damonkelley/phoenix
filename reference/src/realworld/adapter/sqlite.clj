(ns realworld.adapter.sqlite
  (:require [clojure.java.io :as io]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]))

(def ^:private migration-resources
  ["migrations/001-create-accounts.sql"
   "migrations/002-create-current-session.sql"
   "migrations/003-create-articles.sql"
   "migrations/004-create-article-tags.sql"])

(defn database [path]
  (jdbc/get-datasource {:dbtype       "sqlite"
                        :dbname       path
                        :foreign_keys true}))

(defn initialize! [database]
  (doseq [migration migration-resources]
    (jdbc/execute!
     database
     [(slurp (io/resource migration))])))

(defn account-by-email [database email]
  (when-let [account
             (jdbc/execute-one!
              database
              ["SELECT id, email, password_hash
                FROM accounts
                WHERE email = ?"
               email]
              {:builder-fn result-set/as-unqualified-lower-maps})]
    {:realworld.account/id            (parse-uuid (:id account))
     :realworld.account/email         (:email account)
     :realworld.account/password-hash (:password_hash account)}))

(defn create-account! [database account]
  (jdbc/execute-one!
   database
   ["INSERT INTO accounts (id, email, password_hash)
     VALUES (?, ?, ?)"
    (str (:realworld.account/id account))
    (:realworld.account/email account)
    (:realworld.account/password-hash account)]))

(defn start-session! [database account]
  (jdbc/execute-one!
   database
   ["INSERT INTO current_session (singleton, account_id)
     VALUES (1, ?)
     ON CONFLICT(singleton) DO UPDATE SET account_id = excluded.account_id"
    (str (:realworld.account/id account))]))

(defn session-account-id [database]
  (some-> (jdbc/execute-one!
           database
           ["SELECT account_id
             FROM current_session
             WHERE singleton = 1"]
           {:builder-fn result-set/as-unqualified-lower-maps})
          :account_id
          parse-uuid))

(defn create-article! [database article]
  (jdbc/with-transaction [transaction database]
    (jdbc/execute-one!
     transaction
     ["INSERT INTO articles
       (slug, author_id, title, description, body, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)"
      (:realworld.article/slug article)
      (str (:realworld.article/author-id article))
      (:realworld.article/title article)
      (:realworld.article/description article)
      (:realworld.article/body article)
      (str (:realworld.article/created-at article))
      (str (:realworld.article/updated-at article))])
    (doseq [[position tag] (map-indexed vector
                                        (:realworld.article/tags article))]
      (jdbc/execute-one!
       transaction
       ["INSERT INTO article_tags (article_slug, position, tag)
         VALUES (?, ?, ?)"
        (:realworld.article/slug article)
        position
        tag]))))

(defn article-by-slug [database slug]
  (when-let [article
             (jdbc/execute-one!
              database
              ["SELECT slug, author_id, title, description, body,
                       created_at, updated_at
                FROM articles
                WHERE slug = ?"
               slug]
              {:builder-fn result-set/as-unqualified-lower-maps})]
    {:realworld.article/slug        (:slug article)
     :realworld.article/author-id   (parse-uuid (:author_id article))
     :realworld.article/title       (:title article)
     :realworld.article/description (:description article)
     :realworld.article/body        (:body article)
     :realworld.article/tags        (mapv :tag
                                          (jdbc/execute!
                                           database
                                           ["SELECT tag
                                             FROM article_tags
                                             WHERE article_slug = ?
                                             ORDER BY position"
                                            slug]
                                           {:builder-fn result-set/as-unqualified-lower-maps}))
     :realworld.article/created-at  (java.time.Instant/parse (:created_at article))
     :realworld.article/updated-at  (java.time.Instant/parse (:updated_at article))}))
