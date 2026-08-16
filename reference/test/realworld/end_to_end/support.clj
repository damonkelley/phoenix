(ns realworld.end-to-end.support
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def executable
  (.getCanonicalPath (io/file "bin/realworld")))

(defn temporary-workspace []
  (.toFile (Files/createTempDirectory "realworld-cli-"
                                      (make-array FileAttribute 0))))

(defn delete-recursively! [file]
  (when (.isDirectory file)
    (doseq [child (.listFiles file)]
      (delete-recursively! child)))
  (Files/deleteIfExists (.toPath file)))

(defn with-workspace [f]
  (let [workspace (temporary-workspace)]
    (try
      (f workspace)
      (finally
        (delete-recursively! workspace)))))

(defn run-command [workspace & arguments]
  (apply shell/sh
         executable
         (concat arguments [:dir (.getPath workspace)])))
