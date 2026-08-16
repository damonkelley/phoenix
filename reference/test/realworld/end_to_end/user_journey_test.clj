(ns realworld.end-to-end.user-journey-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [lazytest.core :refer [defdescribe expect it]]
            [realworld.end-to-end.support :as support]))

(def slug-pattern
  #"hello-world-[0-9a-f]{6}")

(defdescribe user-journey
  (it "registers, logs in, creates articles, and reads the feed"
    (support/with-workspace
      (fn [workspace]
        (let [registered (support/run-command
                          workspace
                          "account" "register"
                          "--email" "alice@example.com"
                          "--password" "secret123")]
          (expect (= 0 (:exit registered)))
          (expect (= "Success\n" (:out registered)))
          (expect (= "" (:err registered)))
          (expect (.isFile (io/file workspace "realworld.db"))))

        (let [logged-in (support/run-command
                         workspace
                         "account" "login"
                         "--email" "ALICE@EXAMPLE.COM"
                         "--password" "secret123")]
          (expect (= 0 (:exit logged-in)))
          (expect (= "Success\n" (:out logged-in)))
          (expect (= "" (:err logged-in))))

        (let [first-article (support/run-command
                             workspace
                             "article" "create"
                             "--title" "Hello World"
                             "--description" "An introduction"
                             "--body" "Article contents"
                             "--tag" "clojure"
                             "--tag" "sqlite")
              first-slug (string/trim (:out first-article))]
          (expect (= 0 (:exit first-article)))
          (expect (re-matches slug-pattern first-slug))
          (expect (= "" (:err first-article)))

          (let [second-article (support/run-command
                                workspace
                                "article" "create"
                                "--title" "Hello World"
                                "--description" "Another introduction"
                                "--body" "More article contents")
                second-slug (string/trim (:out second-article))]
            (expect (= 0 (:exit second-article)))
            (expect (re-matches slug-pattern second-slug))
            (expect (not= first-slug second-slug))
            (expect (= "" (:err second-article)))

            (let [feed (support/run-command workspace "article" "feed")]
              (expect (= 0 (:exit feed)))
              (expect (= (str "TITLE        SLUG                TAGS             AUTHOR\n"
                              "-----------  ------------------  ---------------  -----------------\n"
                              "Hello World  " second-slug "  (none)           alice@example.com\n"
                              "Hello World  " first-slug "  clojure, sqlite  alice@example.com\n")
                         (:out feed)))
              (expect (= "" (:err feed)))))))))

  (it "protects registered identity and authenticated behavior"
    (support/with-workspace
      (fn [workspace]
        (let [feed (support/run-command workspace "article" "feed")]
          (expect (= 0 (:exit feed)))
          (expect (= "No articles\n" (:out feed)))
          (expect (= "" (:err feed))))

        (let [registered (support/run-command
                          workspace
                          "account" "register"
                          "--email" "alice@example.com"
                          "--password" "secret123")]
          (expect (= 0 (:exit registered))))

        (let [duplicate (support/run-command
                         workspace
                         "account" "register"
                         "--email" "Alice@Example.com"
                         "--password" "secret123")]
          (expect (= 1 (:exit duplicate)))
          (expect (= "" (:out duplicate)))
          (expect (= "Email is already taken\n" (:err duplicate))))

        (doseq [[email password]
                [["missing@example.com" "short "]
                 ["alice@example.com" "short "]]]
          (let [login (support/run-command
                       workspace
                       "account" "login"
                       "--email" email
                       "--password" password)]
            (expect (= 1 (:exit login)))
            (expect (= "" (:out login)))
            (expect (= "Email or password is invalid\n" (:err login)))))

        (let [article (support/run-command
                       workspace
                       "article" "create"
                       "--title" "Hello World"
                       "--description" "An introduction"
                       "--body" "Article contents")]
          (expect (= 1 (:exit article)))
          (expect (= "" (:out article)))
          (expect (= "Login is required\n" (:err article)))))))

  (it "reports applicable input errors throughout the journey"
    (support/with-workspace
      (fn [workspace]
        (let [registration (support/run-command
                            workspace
                            "account" "register"
                            "--email" "alice..smith@example-.com"
                            "--password" "short ")]
          (expect (= 1 (:exit registration)))
          (expect (= "" (:out registration)))
          (expect (= #{"Email is invalid"
                       "Password must be at least 8 characters"
                       "Password must not contain whitespace"}
                     (set (string/split-lines (:err registration))))))

        (let [login (support/run-command
                     workspace
                     "account" "login"
                     "--email" "alice..smith@example.com"
                     "--password" "")]
          (expect (= 1 (:exit login)))
          (expect (= "" (:out login)))
          (expect (= #{"Email is invalid" "Password is required"}
                     (set (string/split-lines (:err login))))))

        (let [article (support/run-command
                       workspace
                       "article" "create"
                       "--title" " "
                       "--description" ""
                       "--body" "\t"
                       "--tag" "")]
          (expect (= 1 (:exit article)))
          (expect (= "" (:out article)))
          (expect (= #{"Title is required"
                       "Description is required"
                       "Body is required"
                       "Tag must not be blank"}
                     (set (string/split-lines (:err article))))))))))
