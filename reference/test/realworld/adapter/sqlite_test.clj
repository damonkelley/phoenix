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

  (describe "active account"
    (it "persists and replaces the active account"
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
          (expect (nil? (sqlite/active-account-id database)))

          (sqlite/activate-account! database first-account)
          (expect (= first-id (sqlite/active-account-id database)))

          (sqlite/initialize! database)
          (expect (= first-id (sqlite/active-account-id database)))

          (sqlite/activate-account! database second-account)
          (expect (= second-id
                     (sqlite/active-account-id (sqlite/database path))))

          (expect (try
                    (sqlite/activate-account!
                     database
                     {:realworld.account/id
                      (parse-uuid "00000000-0000-0000-0000-000000000003")})
                    false
                    (catch SQLException _
                      true)))
          (finally
            (delete-database! path)))))))
