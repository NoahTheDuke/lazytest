(ns lazytest.repl
  (:require
    [lazytest.config :refer [->config]]
    [lazytest.results :refer [summarize]]
    [lazytest.runner :as runner]
    [lazytest.reporters :as r]))

(def repl-config
  {:reporter [r/focused r/results r/summary]})

(defn run-tests
  "Runs tests defined in the given namespaces."
  ([namespaces] (run-tests namespaces repl-config))
  ([namespaces config]
   (if (sequential? namespaces)
     (summarize (runner/run-tests namespaces (->config config)))
     (run-tests [namespaces] config))))

(defn run-all-tests
  "Run tests defined in all namespaces."
  ([] (run-all-tests repl-config))
  ([config]
   (summarize (runner/run-all-tests (->config config)))))

(defn run-test-var
  ([v] (run-test-var v repl-config))
  ([v config]
   (summarize (runner/run-test-var v (->config config)))))
