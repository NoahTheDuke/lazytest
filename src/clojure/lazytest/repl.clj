(ns lazytest.repl
  (:require
   [lazytest.config :refer [->config]]
   [lazytest.reporters :as r]
   [lazytest.results :refer [summarize]]
   [lazytest.runner :as runner]))

(set! *warn-on-reflection* true)

(def repl-config
  {:output [r/focused r/results r/summary]})

(defn run-tests
  "Runs tests defined in the given namespaces."
  {:arglists '([namespaces] [namespaces {:keys [output] :as config}])}
  ([namespaces] (run-tests namespaces repl-config))
  ([namespaces config]
   (if (sequential? namespaces)
     (summarize (runner/run-tests namespaces (->config config)))
     (run-tests [namespaces] config))))

(defn run-all-tests
  "Run tests defined in all namespaces."
  {:arglists '([] [{:keys [output] :as config}])}
  ([] (run-all-tests repl-config))
  ([config]
   (summarize (runner/run-all-tests (->config config)))))

(defn run-test-var
  "Run test var."
  {:arglists '([var] [var {:keys [output] :as config}])}
  ([v] (run-test-var v repl-config))
  ([v config]
   (summarize (runner/run-test-var v (->config config)))))
