(ns realworld.adapter.cli.main-test
  (:require [clojure.test :refer [deftest is testing]]
            [realworld.adapter.cli.main :as cli]))

(deftest example
  (testing "dispatches registration arguments"
    (let [result (cli/run ["register"
                           "--email" "alice@example.com"
                           "--password" "secret123"])]
      (is (= 0 (:exit result)))
      (is (= {:command :register
              :options {:email "alice@example.com"
                        :password "secret123"}}
             (:result result)))))

  (testing "rejects unknown options"
    (let [result (cli/run ["register" "--unknown" "value"])]
      (is (= 2 (:exit result)))
      (is (string? (:error result)))))

  (testing "rejects unknown commands"
    (let [result (cli/run ["unknown"])]
      (is (= 2 (:exit result)))
      (is (string? (:error result))))))
