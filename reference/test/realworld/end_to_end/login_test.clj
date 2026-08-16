(ns realworld.end-to-end.login-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [lazytest.core :refer [defdescribe describe expect it]])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def executable
  (.getCanonicalPath (io/file "bin/realworld")))

(defn temporary-directory []
  (.toFile (Files/createTempDirectory "realworld-cli-"
                                      (make-array FileAttribute 0))))

(defn delete-recursively! [file]
  (when (.isDirectory file)
    (doseq [child (.listFiles file)]
      (delete-recursively! child)))
  (Files/deleteIfExists (.toPath file)))

(defdescribe login
  (describe "login command"
    (it "authenticates a registered account case-insensitively"
      (let [directory (temporary-directory)]
        (try
          (let [registered (shell/sh executable
                                     "register"
                                     "--email" "alice@example.com"
                                     "--password" "secret123"
                                     :dir (.getPath directory))
                logged-in (shell/sh executable
                                    "login"
                                    "--email" "ALICE@EXAMPLE.COM"
                                    "--password" "secret123"
                                    :dir (.getPath directory))]
            (expect (= 0 (:exit registered)))
            (expect (= 0 (:exit logged-in)))
            (expect (= "Success\n" (:out logged-in)))
            (expect (= "" (:err logged-in))))
          (finally
            (delete-recursively! directory)))))

    (it "does not distinguish a missing account from an incorrect password"
      (let [directory (temporary-directory)]
        (try
          (let [registered (shell/sh executable
                                     "register"
                                     "--email" "alice@example.com"
                                     "--password" "secret123"
                                     :dir (.getPath directory))
                missing-account (shell/sh executable
                                          "login"
                                          "--email" "missing@example.com"
                                          "--password" "short "
                                          :dir (.getPath directory))
                incorrect-password (shell/sh executable
                                             "login"
                                             "--email" "alice@example.com"
                                             "--password" "short "
                                             :dir (.getPath directory))]
            (expect (= 0 (:exit registered)))
            (doseq [result [missing-account incorrect-password]]
              (expect (= 1 (:exit result)))
              (expect (= "" (:out result)))
              (expect (= "Email or password is invalid\n" (:err result)))))
          (finally
            (delete-recursively! directory)))))

    (it "reports all applicable validation errors"
      (let [directory (temporary-directory)]
        (try
          (let [result (shell/sh executable
                                 "login"
                                 "--email" "alice..smith@example.com"
                                 "--password" ""
                                 :dir (.getPath directory))]
            (expect (= 1 (:exit result)))
            (expect (= "" (:out result)))
            (expect (= #{"Email is invalid" "Password is required"}
                       (set (string/split-lines (:err result))))))
          (finally
            (delete-recursively! directory)))))))
