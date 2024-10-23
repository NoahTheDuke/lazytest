(ns lazytest.cli
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [lazytest.config :refer [lazytest-version]]))

(defn update-vec [m k v]
  (update m k #(conj (or % []) v)))

(defn update-set [m k v]
  (update m k #(conj (or % #{}) v)))

(def cli-options
  [["-d" "--dir DIR" "Directory containing tests."
    :assoc-fn update-vec]
   ["-n" "--namespace SYMBOL" "Run only the specified test namespaces. Can be given multiple times."
    :id :ns-filter
    :parse-fn symbol
    :assoc-fn update-set]
   ["-v" "--var SYMBOL" "Run only the specified fully-qualified symbol."
    :id :var-filter
    :parse-fn symbol
    :assoc-fn update-set]
   ["-i" "--include KEYWORD" "Run only test sequences or vars with this metadata keyword."
    :parse-fn #(keyword (if (str/starts-with? % ":") (subs % 1) %))
    :assoc-fn update-set]
   ["-e" "--exclude KEYWORD" "Exclude test sequences or vars with this metadata keyword."
    :parse-fn #(keyword (if (str/starts-with? % ":") (subs % 1) %))
    :assoc-fn update-set]
   [nil "--output SYMBOL" "Output format. Can be given multiple times. (Defaults to \"nested\".)"
    :parse-fn read-string
    :assoc-fn (fn [args k v]
                (let [output (if (qualified-symbol? v)
                               v
                               (symbol "lazytest.reporters" (name v)))]
                  (update-vec args k output)))]
   [nil "--md FILE" "Run doctests for given markdown file. Can be given multiple times."
    :parse-fn io/file
    :assoc-fn update-vec]
   [nil "--doctests" "Run doctests for vars and markdown files found in paths."]
   [nil "--watch" "Run under Watch mode. Uses clj-reload to reload changed and dependent namespaces, then reruns test suite."]
   [nil "--delay NUM" "(Watch mode) How many milliseconds to wait before checking for changes. (Defaults to 500.)"
    :parse-fn parse-long]
   [nil "--help" "Print help information."]
   [nil "--version" "Print version information."]])

(defn help-message
  [specs]
  (let [lines [(lazytest-version)
               ""
               "Usage:"
               "  lazytest [options]"
               "  lazytest [options] [path...]"
               ""
               "Options:"
               (cli/summarize specs)
               ""
               "If neither paths nor `--dir` are provided, lazytest will search `test/`."
               ""]]
    {:exit-message (str/join \newline lines)
     :ok true}))

(defn print-errors
  [errors]
  {:exit-message (str/join \newline (cons "lazytest errors:" errors))
   :ok false})

(defn prepare-output [output]
  (->> output
       (distinct)
       (vec)))

(defn validate-opts
  "Parse and validate opts.

  Returns either a map of {:exit-message \"some str\" :ok logical-boolean}
  or {map of cli opts}.

  :ok is false if given invalid options."
  [opts]
  (let [{:keys [options errors summary arguments]}
        (cli/parse-opts opts cli-options :strict true :summary-fn identity)]
    (cond
      (:help options) (help-message summary)
      (:version options) {:exit-message "lazytest 0.0" :ok true}
      errors (print-errors errors)
      :else (-> options
                (update :dir (comp vec concat) arguments)
                (update :output prepare-output)
                (update :delay #(or % 500))))))
