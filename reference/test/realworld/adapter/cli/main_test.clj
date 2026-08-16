(ns realworld.adapter.cli.main-test
  (:require [lazytest.core :refer [defdescribe describe expect it]]
            [realworld.adapter.cli.main :as cli]
            [realworld.response :as response]))

(defn unexpected-dispatch [_command]
  (throw (ex-info "Unexpected dispatch" {})))

(defdescribe command-line-interface
  (describe "register command"
    (it "dispatches registration arguments"
      (let [dispatched (atom nil)
            result (cli/run
                    ["register"
                     "--email" "alice@example.com"
                     "--password" "12345678"]
                    (fn [command]
                      (reset! dispatched command)
                      {:realworld.application/response
                       (response/ok)}))]
        (expect (= {:realworld.command/name :realworld.account/register
                    :realworld.command/parameters
                    {:realworld.account/email    "alice@example.com"
                     :realworld.account/password "12345678"}}
                   @dispatched))
        (expect (= 0 (:exit result)))
        (expect (= "Success" (:output result)))))

    (it "rejects unknown options"
      (let [result (cli/run ["register" "--unknown" "value"]
                            unexpected-dispatch)]
        (expect (= 2 (:exit result)))
        (expect (string? (:error result))))))

  (describe "unknown command"
    (it "is rejected as malformed usage"
      (let [result (cli/run ["unknown"] unexpected-dispatch)]
        (expect (= 2 (:exit result)))
        (expect (string? (:error result)))))))
