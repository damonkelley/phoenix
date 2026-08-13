(ns realworld.test-runner
  (:require [clojure.test :as test]
            [realworld.adapter.cli.main-test]))

(defn -main [& _]
  (let [{:keys [fail error]} (test/run-tests 'realworld.adapter.cli.main-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
