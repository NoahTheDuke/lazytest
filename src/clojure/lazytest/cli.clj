(ns lazytest.cli
  (:require
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [lazytest.config :refer [lazytest-version]]))

(defn update-args [m k v]
  (update m k #(conj (or % []) v)))

(defn update-set [m k v]
  (update m k #(conj (or % #{}) v)))

(def cli-options
  [["-d" "--dir DIR" "Directory containing tests. (Defaults to \"test\".)"
    :assoc-fn update-args]
   ["-n" "--namespace NS-SYM" "Run only the specified test namespaces. Can be given multiple times."
    :id :ns-filter
    :parse-fn symbol
    :assoc-fn update-set]
   ["-v" "--var VAR-SYM" "Run only the specified fully-qualified symbol."
    :id :var-filter
    :parse-fn symbol
    :assoc-fn update-set]
   ["-i" "--include KEYWORD" "Run only test sequences or vars with this metadata keyword."
    :parse-fn #(keyword (if (str/starts-with? % ":") (subs % 1) %))
    :assoc-fn update-set]
   ["-e" "--exclude KEYWORD" "Exclude test sequences or vars with this metadata keyword."
    :parse-fn #(keyword (if (str/starts-with? % ":") (subs % 1) %))
    :assoc-fn update-set]
   [nil "--output OUTPUT" "Output format. Can be given multiple times. (Defaults to \"nested\".)"
    :parse-fn read-string
    :assoc-fn update-args]
   [nil "--help" "Print help information."]
   [nil "--version" "Print version information."]])

(defn help-message
  [specs]
  (let [lines [(lazytest-version)
               ""
               "Usage:"
               "  lazytest [options]"
               ""
               "Options:"
               (#'cli/summarize specs)
               ""]]
    {:exit-message (str/join \newline lines)
     :ok true}))

(defn print-errors
  [errors]
  {:exit-message (str/join \newline (cons "lazytest errors:" errors))
   :ok false})

(defn prepare-output [options]
  (update options :output
          (fn [output]
            (if output
              (->> output
                   (map #(if (qualified-symbol? %) % (symbol "lazytest.reporters" (name %))))
                   (distinct)
                   (vec))
              ['lazytest.reporters/nested]))))

(defn validate-opts
  "Parse and validate opts.

  Returns either a map of {:exit-message \"some str\" :ok logical-boolean}
  or {map of cli opts}.

  :ok is false if given invalid options."
  [opts]
  (let [{:keys [options errors summary]}
        (cli/parse-opts opts cli-options :strict true :summary-fn identity)]
    (cond
      (:help options) (help-message summary)
      (:version options) {:exit-message "lazytest 0.0" :ok true}
      errors (print-errors errors)
      :else (-> options
                (prepare-output)))))
