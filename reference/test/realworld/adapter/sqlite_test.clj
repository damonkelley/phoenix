(ns realworld.adapter.sqlite-test
  (:require [lazytest.core :refer [defdescribe describe expect it]]
            [realworld.adapter.sqlite :as sqlite])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.sql SQLException]))

(defn temporary-database-path []
  (str (Files/createTempFile "realworld-" ".db"
                             (make-array FileAttribute 0))))

(defn delete-database! [path]
  (Files/deleteIfExists (.toPath (java.io.File. path))))

(defdescribe sqlite-adapter
  (describe "accounts"
    (it "creates an account and finds it by email case-insensitively"
      (let [path (temporary-database-path)
            database (sqlite/database path)
            account {:realworld.account/id            (parse-uuid "00000000-0000-0000-0000-000000000001")
                     :realworld.account/email         "alice@example.com"
                     :realworld.account/password-hash "password-hash"}]
        (try
          (sqlite/initialize! database)
          (sqlite/create-account! database account)
          (expect (= account
                     (sqlite/account-by-email database
                                              "ALICE@EXAMPLE.COM")))
          (expect (try
                    (sqlite/create-account!
                     database
                     (assoc account
                            :realworld.account/id (parse-uuid "00000000-0000-0000-0000-000000000002")
                            :realworld.account/email "Alice@example.com"))
                    false
                    (catch SQLException _
                      true)))
          (finally
            (delete-database! path))))))

  (describe "session"
    (it "persists and replaces the authenticated account"
      (let [path (temporary-database-path)
            database (sqlite/database path)
            first-id (parse-uuid "00000000-0000-0000-0000-000000000001")
            second-id (parse-uuid "00000000-0000-0000-0000-000000000002")
            first-account {:realworld.account/id            first-id
                           :realworld.account/email         "alice@example.com"
                           :realworld.account/password-hash "first-hash"}
            second-account {:realworld.account/id            second-id
                            :realworld.account/email         "bob@example.com"
                            :realworld.account/password-hash "second-hash"}]
        (try
          (sqlite/initialize! database)
          (sqlite/create-account! database first-account)
          (sqlite/create-account! database second-account)
          (expect (nil? (sqlite/session-account-id database)))

          (sqlite/start-session! database first-account)
          (expect (= first-id (sqlite/session-account-id database)))

          (sqlite/initialize! database)
          (expect (= first-id (sqlite/session-account-id database)))

          (sqlite/start-session! database second-account)
          (expect (= second-id
                     (sqlite/session-account-id (sqlite/database path))))

          (expect (try
                    (sqlite/start-session!
                     database
                     {:realworld.account/id
                      (parse-uuid "00000000-0000-0000-0000-000000000003")})
                    false
                    (catch SQLException _
                      true)))
          (finally
            (delete-database! path))))))

  (describe "articles"
    (it "persists an article and its tags"
      (let [path (temporary-database-path)
            database (sqlite/database path)
            account {:realworld.account/id
                     (parse-uuid "00000000-0000-0000-0000-000000000001")
                     :realworld.account/email         "alice@example.com"
                     :realworld.account/password-hash "password-hash"}
            now (java.time.Instant/parse "2026-08-16T12:00:00Z")
            article {:realworld.article/slug        "hello-world"
                     :realworld.article/author-id   (:realworld.account/id account)
                     :realworld.article/title       "Hello World"
                     :realworld.article/description "An introduction"
                     :realworld.article/body        "Article contents"
                     :realworld.article/tags        ["clojure" "sqlite"]
                     :realworld.article/created-at  now
                     :realworld.article/updated-at  now}]
        (try
          (sqlite/initialize! database)
          (sqlite/create-account! database account)
          (sqlite/create-article! database article)
          (expect (= (assoc article
                            :realworld.article/author
                            {:realworld.account/email "alice@example.com"})
                     (sqlite/article-by-slug (sqlite/database path)
                                             "hello-world")))
          (expect (nil? (sqlite/article-by-slug database "missing")))

          (expect (try
                    (sqlite/create-article! database article)
                    false
                    (catch SQLException _
                      true)))
          (finally
            (delete-database! path)))))

    (it "lists article summaries newest first"
      (let [path (temporary-database-path)
            database (sqlite/database path)
            alice {:realworld.account/id
                   (parse-uuid "00000000-0000-0000-0000-000000000001")
                   :realworld.account/email         "alice@example.com"
                   :realworld.account/password-hash "alice-hash"}
            bob {:realworld.account/id
                 (parse-uuid "00000000-0000-0000-0000-000000000002")
                 :realworld.account/email         "bob@example.com"
                 :realworld.account/password-hash "bob-hash"}
            older {:realworld.article/slug        "older"
                   :realworld.article/author-id   (:realworld.account/id alice)
                   :realworld.article/title       "Older"
                   :realworld.article/description "Older description"
                   :realworld.article/body        "Older body"
                   :realworld.article/tags        ["clojure" "sqlite"]
                   :realworld.article/created-at
                   (java.time.Instant/parse "2026-08-16T12:00:00Z")
                   :realworld.article/updated-at
                   (java.time.Instant/parse "2026-08-16T12:00:00Z")}
            newest {:realworld.article/slug        "newest"
                    :realworld.article/author-id   (:realworld.account/id bob)
                    :realworld.article/title       "Newest"
                    :realworld.article/description "Newest description"
                    :realworld.article/body        "Newest body"
                    :realworld.article/tags        []
                    :realworld.article/created-at
                    (java.time.Instant/parse "2026-08-16T13:00:00Z")
                    :realworld.article/updated-at
                    (java.time.Instant/parse "2026-08-16T13:00:00Z")}]
        (try
          (sqlite/initialize! database)
          (expect (= [] (sqlite/article-feed database)))
          (sqlite/create-account! database alice)
          (sqlite/create-account! database bob)
          (sqlite/create-article! database older)
          (sqlite/create-article! database newest)
          (expect (= [{:realworld.article/title "Newest"
                       :realworld.article/slug  "newest"
                       :realworld.article/tags  []
                       :realworld.article/author
                       {:realworld.account/email "bob@example.com"}}
                      {:realworld.article/title "Older"
                       :realworld.article/slug  "older"
                       :realworld.article/tags  ["clojure" "sqlite"]
                       :realworld.article/author
                       {:realworld.account/email "alice@example.com"}}]
                     (sqlite/article-feed database)))
          (finally
            (delete-database! path)))))))
