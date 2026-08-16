(ns realworld.adapter.sqlite
  (:require [clojure.java.io :as io]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]))

(defn database [path]
  (jdbc/get-datasource {:dbtype "sqlite"
                        :dbname path}))

(defn initialize! [database]
  (jdbc/execute!
   database
   [(slurp (io/resource "migrations/001-create-accounts.sql"))]))

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
