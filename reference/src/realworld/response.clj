(ns realworld.response)

(def ^:private option-keys
  {:data    :realworld.response/data
   :events  :realworld.response/events
   :effects :realworld.response/effects})

(defn- build [outcome options]
  (reduce-kv
   (fn [response key value]
     (if-let [response-key (option-keys key)]
       (assoc response response-key value)
       (throw (ex-info "Unknown response option" {:option key}))))
   {:realworld.response/outcome outcome}
   options))

(defn ok [& {:as options}]
  (build :ok options))

(defn error [& {:as options}]
  (build :error options))
