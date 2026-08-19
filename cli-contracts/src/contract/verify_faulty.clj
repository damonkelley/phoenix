(ns contract.verify-faulty
  (:require [clojure.java.io :as io]
            [contract.runner :as runner]))

(defn- project-root []
  (-> (io/resource "contract/verify_faulty.clj")
      io/file
      .toPath
      .getParent
      .getParent
      .getParent))

(def ^:private candidates
  [{:name "always-success"
    :file "always-success"
    :scenario :email-grammar
    :check "reject address missing @"
    :violation "it reports success for an invalid email"}
   {:name "first-error-only"
    :file "first-error-only"
    :scenario :password-and-validation
    :check "report both required errors"
    :violation "it omits an applicable validation line"}
   {:name "case-sensitive-identity-persistence"
    :file "case-sensitive-identity-persistence"
    :scenario :atomicity-identity-and-precedence
    :check "reject valid case-varied duplicate"
    :violation "it treats case-varied email identity as distinct"}])

(defn- exception-failure? [failure]
  (= :exception (:kind failure)))

(defn- verify-candidate [{:keys [name file scenario check violation]}]
  (try
    (let [target (runner/resolve-target
                   (str (.resolve (project-root) (str "faulty/" file))))
          canonical (:failures
                     (runner/evaluate-scenario target :successful-registration))
          discriminatory (:failures (runner/evaluate-scenario target scenario))
          intended? (some #(= check (:check %)) discriminatory)
          launch-failure? (some exception-failure? discriminatory)]
      (if (empty? canonical)
        (println (str "PASS " name
                      ": launchable and accepted by canonical success"))
        (do
          (println (str "FAIL " name ": canonical success was rejected"))
          (doseq [failure canonical]
            (println (str "  " (pr-str failure))))))
      (if (and intended? (not launch-failure?))
        (println (str "PASS " name ": rejected because " violation))
        (do
          (println (str "FAIL " name
                        ": was not rejected solely through the intended behavior"))
          (doseq [failure discriminatory]
            (println (str "  " (pr-str failure))))))
      (and (empty? canonical) intended? (not launch-failure?)))
    (catch Throwable error
      (println (str "FAIL " name ": candidate could not be launched: "
                    (or (.getMessage error) (str (class error)))))
      false)))

(defn -main [& args]
  (when (seq args)
    (binding [*out* *err*]
      (println "Usage: bb verify-faulty"))
    (System/exit 2))
  (let [results (mapv verify-candidate candidates)]
    (if (every? true? results)
      (println "\n3 faulty candidates verified")
      (System/exit 1))))
