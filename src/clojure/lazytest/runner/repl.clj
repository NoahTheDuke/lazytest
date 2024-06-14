(ns lazytest.runner.repl
  (:require
    [lazytest.results :refer [summarize]]
    [lazytest.runner :as runner :refer [->context]]
    [malli.experimental :as mx]
    [lazytest.reporters :as r]))

(def repl-context
  (->context {:reporter [r/results r/summary]}))

(defn run-tests
  "Runs tests defined in the given namespaces."
  [& namespaces]
  (summarize (runner/run-tests repl-context namespaces)))

(defn run-all-tests
  "Run tests defined in all namespaces."
  []
  (summarize (runner/run-all-tests repl-context)))

(mx/defn run-test-var
  [v :- :lt/var]
  (summarize (runner/run-test-var repl-context v)))
