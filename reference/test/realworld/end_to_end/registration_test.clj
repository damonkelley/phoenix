(ns realworld.end-to-end.registration-test
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

(defdescribe registration
  (describe "register command"
    (it "persists an account and rejects a case-insensitive duplicate"
      (let [directory (temporary-directory)]
        (try
          (let [registered (shell/sh executable
                                     "register"
                                     "--email" "alice@example.com"
                                     "--password" "secret123"
                                     :dir (.getPath directory))
                duplicate (shell/sh executable
                                    "register"
                                    "--email" "Alice@Example.com"
                                    "--password" "secret123"
                                    :dir (.getPath directory))]
            (expect (= 0 (:exit registered)))
            (expect (= "Success\n" (:out registered)))
            (expect (= "" (:err registered)))
            (expect (.isFile (io/file directory "realworld.db")))
            (expect (= 1 (:exit duplicate)))
            (expect (= "" (:out duplicate)))
            (expect (= "Email is already taken\n" (:err duplicate))))
          (finally
            (delete-recursively! directory)))))

    (it "reports all applicable validation errors"
      (let [directory (temporary-directory)]
        (try
          (let [result (shell/sh executable
                                 "register"
                                 "--email" "alice..smith@example-.com"
                                 "--password" "short "
                                 :dir (.getPath directory))]
            (expect (= 1 (:exit result)))
            (expect (= "" (:out result)))
            (expect (= #{"Email is invalid"
                         "Password must be at least 8 characters"
                         "Password must not contain whitespace"}
                       (set (string/split-lines (:err result))))))
          (finally
            (delete-recursively! directory)))))))
