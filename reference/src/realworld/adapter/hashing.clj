(ns realworld.adapter.hashing
  (:require [buddy.hashers :as hashers]))

(def ^:private options
  {:alg :bcrypt+sha512})

(defn hash-password [password]
  (hashers/derive password options))

(defn matches? [password password-hash]
  (:valid (hashers/verify password password-hash)))
