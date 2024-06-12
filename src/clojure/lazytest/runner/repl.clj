(ns lazytest.runner.repl
  (:require
    [lazytest.results :refer [summarize]]
    [lazytest.runner :as runner]
    [malli.experimental :as mx]))

(defn run-tests
  "Runs tests defined in the given namespaces."
  [& namespaces]
  (let [results (runner/run-tests namespaces)]
    (summarize results)))

(defn run-all-tests
  "Run tests defined in all namespaces."
  []
  (let [results (runner/run-all-tests)]
    (summarize results)))

(mx/defn run-test-var
  [v :- :lt/var]
  (let [results (runner/run-test-var v)]
    (summarize results)))
