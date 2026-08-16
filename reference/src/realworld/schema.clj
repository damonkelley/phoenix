(ns realworld.schema
  (:require [clojure.string :as string]
            [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]))

(def ^:private missing-key-errors
  (assoc me/default-errors
         ::m/missing-key
         {:error/fn
          (fn [{:keys [in]} _]
            (str (-> in
                     last
                     name
                     (string/replace "-" " ")
                     string/capitalize)
                 " is required"))}))

(defn valid? [schema value]
  (m/validate schema value))

(defn validate [schema value]
  (when-let [explanation (m/explain schema value)]
    (->> (me/humanize explanation {:errors missing-key-errors})
         (tree-seq coll? seq)
         (filter string?)
         set)))

(defn generator [schema]
  (mg/generator schema))
