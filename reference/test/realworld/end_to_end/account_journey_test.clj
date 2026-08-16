(ns realworld.end-to-end.account-journey-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [lazytest.core :refer [defdescribe expect it]]
            [realworld.end-to-end.support :as support]))

(defdescribe account-journey
  (it "registers and logs into an account"
    (support/with-workspace
      (fn [workspace]
        (let [registered (support/run-command
                          workspace
                          "register"
                          "--email" "alice@example.com"
                          "--password" "secret123")
              logged-in (support/run-command
                         workspace
                         "login"
                         "--email" "ALICE@EXAMPLE.COM"
                         "--password" "secret123")]
          (expect (= 0 (:exit registered)))
          (expect (= "Success\n" (:out registered)))
          (expect (= "" (:err registered)))
          (expect (.isFile (io/file workspace "realworld.db")))
          (expect (= 0 (:exit logged-in)))
          (expect (= "Success\n" (:out logged-in)))
          (expect (= "" (:err logged-in)))))))

  (it "protects registered identity and credentials"
    (support/with-workspace
      (fn [workspace]
        (let [registered (support/run-command
                          workspace
                          "register"
                          "--email" "alice@example.com"
                          "--password" "secret123")
              duplicate (support/run-command
                         workspace
                         "register"
                         "--email" "Alice@Example.com"
                         "--password" "secret123")
              missing-account (support/run-command
                               workspace
                               "login"
                               "--email" "missing@example.com"
                               "--password" "short ")
              incorrect-password (support/run-command
                                  workspace
                                  "login"
                                  "--email" "alice@example.com"
                                  "--password" "short ")]
          (expect (= 0 (:exit registered)))
          (expect (= 1 (:exit duplicate)))
          (expect (= "" (:out duplicate)))
          (expect (= "Email is already taken\n" (:err duplicate)))
          (doseq [result [missing-account incorrect-password]]
            (expect (= 1 (:exit result)))
            (expect (= "" (:out result)))
            (expect (= "Email or password is invalid\n" (:err result))))))))

  (it "reports applicable account input errors"
    (support/with-workspace
      (fn [workspace]
        (let [registration (support/run-command
                            workspace
                            "register"
                            "--email" "alice..smith@example-.com"
                            "--password" "short ")
              login (support/run-command
                     workspace
                     "login"
                     "--email" "alice..smith@example.com"
                     "--password" "")]
          (expect (= 1 (:exit registration)))
          (expect (= "" (:out registration)))
          (expect (= #{"Email is invalid"
                       "Password must be at least 8 characters"
                       "Password must not contain whitespace"}
                     (set (string/split-lines (:err registration)))))
          (expect (= 1 (:exit login)))
          (expect (= "" (:out login)))
          (expect (= #{"Email is invalid" "Password is required"}
                     (set (string/split-lines (:err login))))))))))
