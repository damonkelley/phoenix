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
            (delete-database! path)))))))
