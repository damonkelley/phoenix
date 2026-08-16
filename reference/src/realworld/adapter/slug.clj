(ns realworld.adapter.slug
  (:require [clojure.string :as string]))

(defn- base [title]
  (let [slug (-> title
                 string/lower-case
                 (string/replace #"[^a-z0-9]+" "-")
                 (string/replace #"(^-+|-+$)" ""))]
    (if (seq slug)
      slug
      "article")))

(defn from-title [title id]
  (str (base title) "-" (subs (str id) 0 6)))
