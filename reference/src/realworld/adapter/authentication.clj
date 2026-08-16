(ns realworld.adapter.authentication
  (:require [realworld.adapter.hashing :as hashing]
            [realworld.adapter.sqlite :as sqlite]))

(defn account-by-credentials [database email password]
  (when-let [account (sqlite/account-by-email database email)]
    (when (hashing/matches? password
                            (:realworld.account/password-hash account))
      (select-keys account
                   [:realworld.account/id
                    :realworld.account/email]))))
