(ns realworld.adapter.sqlite
  (:require [clojure.java.io :as io]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]))

(def ^:private migration-resources
  ["migrations/001-create-accounts.sql"
   "migrations/002-create-active-account.sql"])

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

(defn activate-account! [database account]
  (jdbc/execute-one!
   database
   ["INSERT INTO active_account (singleton, account_id)
     VALUES (1, ?)
     ON CONFLICT(singleton) DO UPDATE SET account_id = excluded.account_id"
    (str (:realworld.account/id account))]))

(defn active-account-id [database]
  (some-> (jdbc/execute-one!
           database
           ["SELECT account_id
             FROM active_account
             WHERE singleton = 1"]
           {:builder-fn result-set/as-unqualified-lower-maps})
          :account_id
          parse-uuid))
