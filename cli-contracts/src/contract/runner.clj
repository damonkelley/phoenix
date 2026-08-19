(ns contract.runner
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as str])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:private usage "Usage: bb test TARGET_EXECUTABLE")
(def ^:private temp-prefix "phoenix-cli-contract-")

(defn resolve-target
  "Resolve and validate a caller-supplied target before any scenario changes cwd."
  [target]
  (let [path (-> target fs/path .toAbsolutePath .normalize)]
    (cond
      (not (fs/exists? path))
      (throw (ex-info (str "target does not exist: " path) {:usage? true}))

      (not (fs/regular-file? path))
      (throw (ex-info (str "target is not a regular file: " path) {:usage? true}))

      (not (Files/isExecutable path))
      (throw (ex-info (str "target is not executable: " path) {:usage? true}))

      :else path)))

(defn- create-workspace []
  (Files/createTempDirectory temp-prefix (make-array FileAttribute 0)))

(defn- delete-workspaces! [workspaces]
  (let [errors (atom [])]
    (doseq [workspace (reverse workspaces)]
      (try
        (fs/delete-tree workspace)
        (catch Throwable error
          (swap! errors conj error))))
    (when-let [error (first @errors)]
      (throw error))))

(defn- with-clean-workspaces [count-workspaces f]
  (let [workspaces (atom [])]
    (try
      (dotimes [_ count-workspaces]
        (swap! workspaces conj (create-workspace)))
      (f @workspaces)
      (finally
        (delete-workspaces! @workspaces)))))

(defn- invoke [target cwd argv]
  (let [{:keys [exit out err]}
        @(process/process (into [(str target)] argv)
                          {:dir (str cwd)
                           :out :string
                           :err :string})]
    {:exit exit
     :stdout out
     :stderr err}))

(defn- logical-lines [text]
  (let [without-crlf (str/replace text "\r\n" "")]
    (if (str/includes? without-crlf "\r")
      {:error "contains a carriage return that is not part of CRLF"}
      (let [normalized (str/replace text "\r\n" "\n")]
        (cond
          (empty? normalized)
          {:lines []}

          :else
          (let [body (if (str/ends-with? normalized "\n")
                       (subs normalized 0 (dec (count normalized)))
                       normalized)]
            {:lines (if (empty? body)
                      [""]
                      (str/split body #"\n" -1))}))))))

(defn- fail! [failures scenario check field expected actual & [note]]
  (swap! failures conj
         (cond-> {:scenario scenario
                  :check check
                  :field field
                  :expected expected
                  :actual actual}
           note (assoc :note note))))

(defn- expect-equal! [failures scenario check field expected actual]
  (when-not (= expected actual)
    (fail! failures scenario check field expected actual)))

(defn- expect-lines! [failures scenario check field expected text]
  (let [{:keys [lines error]} (logical-lines text)]
    (cond
      error
      (fail! failures scenario check field expected text error)

      (not= (frequencies expected) (frequencies lines))
      (fail! failures scenario check field expected text
             (str "actual logical lines: " (pr-str lines))))))

(defn- expect-result! [failures scenario check actual expected]
  (expect-equal! failures scenario check :exit (:exit expected) (:exit actual))
  (expect-lines! failures scenario check :stdout (:stdout expected) (:stdout actual))
  (expect-lines! failures scenario check :stderr (:stderr expected) (:stderr actual)))

(def ^:private success
  {:exit 0 :stdout ["Success"] :stderr []})

(defn- validation [& lines]
  {:exit 1 :stdout [] :stderr (vec lines)})

(def ^:private duplicate
  (validation "Email is already taken"))

(defn- run-cases! [failures scenario target cwd cases]
  (doseq [{:keys [check argv expected]} cases]
    (expect-result! failures scenario check (invoke target cwd argv) expected)))

(defn- successful-registration [target scenario]
  (with-clean-workspaces
    1
    (fn [[cwd]]
      (let [failures (atom [])]
        (run-cases!
          failures scenario target cwd
          [{:check "canonical registration with an eight-character password"
            :argv ["account" "register"
                   "--email" "alice@example.com"
                   "--password" "12345678"]
            :expected success}])
        @failures))))

(defn- email-grammar [target scenario]
  (with-clean-workspaces
    1
    (fn [[cwd]]
      (let [failures (atom [])
            accepted
            [{:check "accept canonical address"
              :email "alice@example.com"}
             {:check "accept one-label domain"
              :email "local@localhost"}
             {:check "accept every documented local-part character"
              :email "A.z_9%+-@example.com"}
             {:check "accept internal domain-label hyphen"
              :email "alice@example-domain.com"}
             {:check "accept reserved nonexistent domain"
              :email "nobody@definitely-not-a-mailbox.invalid"}]
            rejected
            [{:check "reject address missing @"
              :email "alice.example.com"}
             {:check "reject leading local dot"
              :email ".alice@example.com"}
             {:check "reject trailing local dot"
              :email "alice.@example.com"}
             {:check "reject repeated local dot"
              :email "alice..x@example.com"}
             {:check "reject leading domain-label hyphen"
              :email "alice@-example.com"}
             {:check "reject trailing domain-label hyphen"
              :email "alice@example-.com"}
             {:check "reject empty domain label"
              :email "alice@example..com"}
             {:check "reject email whitespace"
              :email "alice @example.com"}
             {:check "reject non-ASCII address"
              :email "álîçé@example.com"}]
            args (fn [email]
                   ["account" "register"
                    "--email" email
                    "--password" "secret123"])]
        (run-cases! failures scenario target cwd
                    (mapv #(assoc % :argv (args (:email %)) :expected success)
                          accepted))
        (run-cases! failures scenario target cwd
                    (mapv #(assoc %
                                  :argv (args (:email %))
                                  :expected (validation "Email is invalid"))
                          rejected))
        @failures))))

(defn- password-and-validation [target scenario]
  (with-clean-workspaces
    1
    (fn [[cwd]]
      (let [failures (atom [])]
        (run-cases!
          failures scenario target cwd
          [{:check "report missing email"
            :argv ["account" "register" "--password" "secret123"]
            :expected (validation "Email is required")}
           {:check "report missing password"
            :argv ["account" "register" "--email" "missing-password@example.com"]
            :expected (validation "Password is required")}
           {:check "report both required errors"
            :argv ["account" "register"]
            :expected (validation "Email is required" "Password is required")}
           {:check "reject seven-character password"
            :argv ["account" "register"
                   "--email" "seven@example.com"
                   "--password" "1234567"]
            :expected (validation "Password must be at least 8 characters")}
           {:check "accept eight-character password"
            :argv ["account" "register"
                   "--email" "eight@example.com"
                   "--password" "12345678"]
            :expected success}
           {:check "report short and whitespace password errors"
            :argv ["account" "register"
                   "--email" "short-space@example.com"
                   "--password" "abc def"]
            :expected (validation
                        "Password must be at least 8 characters"
                        "Password must not contain whitespace")}
           {:check "reject leading password whitespace without trimming"
            :argv ["account" "register"
                   "--email" "leading-space@example.com"
                   "--password" " secret123"]
            :expected (validation "Password must not contain whitespace")}
           {:check "report invalid email and short password"
            :argv ["account" "register"
                   "--email" "not-an-email"
                   "--password" "short"]
            :expected (validation
                        "Email is invalid"
                        "Password must be at least 8 characters")}])
        @failures))))

(defn- atomicity-identity-and-precedence [target scenario]
  (with-clean-workspaces
    1
    (fn [[cwd]]
      (let [failures (atom [])]
        (run-cases!
          failures scenario target cwd
          [{:check "invalid first attempt"
            :argv ["account" "register"
                   "--email" "Fresh.User@Example.com"
                   "--password" "short"]
            :expected (validation "Password must be at least 8 characters")}
           {:check "valid case-varied retry after failure"
            :argv ["account" "register"
                   "--email" "fresh.user@example.com"
                   "--password" "secret123"]
            :expected success}
           {:check "validation precedes duplicate disclosure"
            :argv ["account" "register"
                   "--email" "FRESH.USER@EXAMPLE.COM"
                   "--password" "short"]
            :expected (validation "Password must be at least 8 characters")}
           {:check "reject valid case-varied duplicate"
            :argv ["account" "register"
                   "--email" "FRESH.User@EXAMPLE.com"
                   "--password" "secret123"]
            :expected duplicate}])
        @failures))))

(defn- persistence-and-cwd-isolation [target scenario]
  (with-clean-workspaces
    2
    (fn [[cwd-a cwd-b]]
      (let [failures (atom [])
            argv ["account" "register"
                  "--email" "same@example.com"
                  "--password" "secret123"]]
        (expect-result! failures scenario "register in cwd A"
                        (invoke target cwd-a argv) success)
        (expect-result! failures scenario "register independently in cwd B"
                        (invoke target cwd-b argv) success)
        (expect-result! failures scenario "retain duplicate state in cwd A"
                        (invoke target cwd-a argv) duplicate)
        (expect-result! failures scenario "retain duplicate state in cwd B"
                        (invoke target cwd-b argv) duplicate)
        @failures))))

(defn- malformed-invocation [target scenario]
  (with-clean-workspaces
    1
    (fn [[cwd]]
      (let [failures (atom [])
            check "classify dangling --password as malformed usage"
            actual (invoke target cwd
                           ["account" "register"
                            "--email" "alice@example.com"
                            "--password"])]
        (expect-equal! failures scenario check :exit 2 (:exit actual))
        (expect-equal! failures scenario check :stdout "" (:stdout actual))
        (when (empty? (:stderr actual))
          (fail! failures scenario check :stderr "a non-empty diagnostic" ""))
        @failures))))

(def scenarios
  [{:id :successful-registration
    :name "Successful registration and process result"
    :run successful-registration}
   {:id :email-grammar
    :name "Documented email grammar and offline validation"
    :run email-grammar}
   {:id :password-and-validation
    :name "Password rules and complete validation reporting"
    :run password-and-validation}
   {:id :atomicity-identity-and-precedence
    :name "Failure atomicity, uniqueness, and validation precedence"
    :run atomicity-identity-and-precedence}
   {:id :persistence-and-cwd-isolation
    :name "Process persistence and cwd isolation"
    :run persistence-and-cwd-isolation}
   {:id :malformed-invocation
    :name "Representative malformed invocation"
    :run malformed-invocation}])

(defn evaluate-scenario [target scenario-id]
  (let [{:keys [id name run] :as scenario}
        (some #(when (= scenario-id (:id %)) %) scenarios)]
    (when-not scenario
      (throw (ex-info (str "unknown scenario: " scenario-id) {})))
    (try
      {:id id :name name :failures (run target id)}
      (catch Throwable error
        {:id id
         :name name
         :failures [{:kind :exception
                     :scenario id
                     :check "scenario execution"
                     :field :exception
                     :expected "scenario completes"
                     :actual (or (.getMessage error) (str (class error)))}]}))))

(defn run-suite [target]
  (mapv #(evaluate-scenario target (:id %)) scenarios))

(defn- print-failure [{:keys [check field expected actual note]}]
  (println (str "    - " check " [" (name field) "]"))
  (println (str "      expected: " (pr-str expected)))
  (println (str "      actual:   " (pr-str actual)))
  (when note
    (println (str "      note:     " note))))

(defn print-results [results]
  (doseq [{:keys [name failures]} results]
    (if (empty? failures)
      (println (str "PASS " name))
      (do
        (println (str "FAIL " name))
        (doseq [failure failures]
          (print-failure failure)))))
  (let [failed (count (filter (comp seq :failures) results))]
    (println (if (zero? failed)
               (str "\n6 scenarios passed")
               (str "\n" failed " of 6 scenarios failed")))
    (zero? failed)))

(defn- usage-error! [message]
  (binding [*out* *err*]
    (println (str "Error: " message))
    (println usage))
  (System/exit 2))

(defn -main [& args]
  (when-not (= 1 (count args))
    (usage-error! "expected exactly one target executable argument"))
  (let [target (try
                 (resolve-target (first args))
                 (catch clojure.lang.ExceptionInfo error
                   (usage-error! (.getMessage error))))
        passed? (print-results (run-suite target))]
    (when-not passed?
      (System/exit 1))))
