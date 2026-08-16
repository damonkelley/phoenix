(ns realworld.response)

(def ^:private option-keys
  {:data    ::data
   :events  ::events
   :effects ::effects})

(defn- build [outcome options]
  (reduce-kv
   (fn [response key value]
     (if-let [response-key (option-keys key)]
       (assoc response response-key value)
       (throw (ex-info "Unknown response option" {:option key}))))
   {::outcome outcome}
   options))

(defn ok [& {:as options}]
  (build :ok options))

(defn error [& {:as options}]
  (build :error options))
