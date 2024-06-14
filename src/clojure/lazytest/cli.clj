(ns lazytest.cli
  (:require
   [clojure.string :as str]
   [clojure.tools.cli :as cli]))

(def fconj
  (fnil conj #{}))

(defn update-args [m k v]
  (update m k fconj v))

(def cli-options
  [["-d" "--dir DIR" "Directory containing tests. (Defaults to \"test\".)"
    :assoc-fn update-args]
   ["-n" "--namespace NS-SYM" "Test namespace to only run."
    :parse-fn symbol
    :assoc-fn update-args]
   ["-v" "--var VAR-SYM" "Test var to only run."
    :parse-fn symbol
    :assoc-fn update-args]
   [nil "--output OUTPUT" "Output format."
    :parse-fn symbol
    :default 'nested]
   ["-h" "--help" "Print help information."]])

(defn help-message
  [specs]
  (let [lines ["lazytest 0.0"
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
      :else options)))
