(ns realworld.adapter.cli.main-test
  (:require [lazytest.core :refer [defdescribe describe expect it]]
            [realworld.adapter.cli.main :as cli]))

(defdescribe command-line-interface
  (describe "register command"
    (it "dispatches registration arguments"
      (let [result (cli/run ["register"
                             "--email" "alice@example.com"
                             "--password" "secret123"])]
        (expect (= 0 (:exit result)))
        (expect (= {:command :register
                    :options {:email    "alice@example.com"
                              :password "secret123"}}
                   (:result result)))))

    (it "rejects unknown options"
      (let [result (cli/run ["register" "--unknown" "value"])]
        (expect (= 2 (:exit result)))
        (expect (string? (:error result))))))

  (describe "unknown command"
    (it "is rejected as malformed usage"
      (let [result (cli/run ["unknown"])]
        (expect (= 2 (:exit result)))
        (expect (string? (:error result)))))))
