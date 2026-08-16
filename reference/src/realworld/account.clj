(ns realworld.account
  (:require [clojure.string :as string]))

(def ^:private email-pattern
  #"[A-Za-z0-9_%+-]+(?:\.[A-Za-z0-9_%+-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*")

(defn- email-present? [email]
  (or (not (string? email))
      (not (string/blank? email))))

(defn- email-valid? [email]
  (or (not (string? email))
      (string/blank? email)
      (boolean (re-matches email-pattern email))))

(def Email
  [:and {:gen/schema [:re email-pattern]}
   [:string {:error/message "Email is invalid"}]
   [:fn {:error/message "Email is required"} email-present?]
   [:fn {:error/message "Email is invalid"} email-valid?]])

(defn- password-present? [password]
  (or (not (string? password))
      (not (empty? password))))

(defn- password-long-enough? [password]
  (or (not (string? password))
      (empty? password)
      (<= 8 (count password))))

(defn- password-without-whitespace? [password]
  (or (not (string? password))
      (empty? password)
      (not (re-find #"\s" password))))

(def Password
  [:and {:gen/schema [:string {:min 8}]}
   [:string {:error/message "Password is required"}]
   [:fn {:error/message "Password is required"} password-present?]
   [:fn {:error/message "Password must be at least 8 characters"}
    password-long-enough?]
   [:fn {:error/message "Password must not contain whitespace"}
    password-without-whitespace?]])

(def RegisterParameters
  [:map
   [:realworld.account/email Email]
   [:realworld.account/password Password]])

(def RegisterCommand
  [:map
   [:realworld.command/name [:= :realworld.account/register]]
   [:realworld.command/parameters RegisterParameters]])

(def command-definitions
  {:realworld.account/register
   {:realworld.command/schema    RegisterCommand
    :realworld.command/coeffects {:realworld.account/id [:realworld.uuid/generate]}
    :realworld.command/handler   (fn [{:realworld.account/keys [id]}
                                      {:realworld.account/keys [email password]}]
                                   {:realworld.response/outcome :ok
                                    :realworld.response/data    {:realworld.account/id id}
                                    :realworld.response/events  [{:realworld.event/type    :realworld.account/registered
                                                                  :realworld.account/id    id
                                                                  :realworld.account/email email}]
                                    :realworld.response/effects [[:realworld.repository/create
                                                                  {:realworld.account/email    email
                                                                   :realworld.account/password password}]]})}})
