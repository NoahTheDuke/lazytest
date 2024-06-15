(ns lazytest.repl
  (:require
    [lazytest.context :refer [->context]]
    [lazytest.results :refer [summarize]]
    [lazytest.runner :as runner]
    [lazytest.reporters :as r]))

(def repl-context
  {:reporter [r/results r/summary]})

(defn run-tests
  "Runs tests defined in the given namespaces."
  ([namespaces] (run-tests namespaces repl-context))
  ([namespaces context]
   (if (sequential? namespaces)
     (summarize (runner/run-tests (->context context) namespaces))
     (run-tests [namespaces] context))))

(defn run-all-tests
  "Run tests defined in all namespaces."
  ([] (run-all-tests repl-context))
  ([context]
   (summarize (runner/run-all-tests (->context context)))))

(defn run-test-var
  ([v] (run-test-var v repl-context))
  ([v context]
   (summarize (runner/run-test-var (->context context) v))))
