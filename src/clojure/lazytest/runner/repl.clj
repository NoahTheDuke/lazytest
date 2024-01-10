(ns lazytest.runner.repl
  (:require
    [lazytest.report.summary :as summary]
    [lazytest.results :refer [summarize]]
    [lazytest.runner :as runner]
    [malli.experimental :as mx]))

(defn run-tests
  "Runs tests defined in the given namespaces."
  [& namespaces]
  (let [results (apply runner/run-tests namespaces)]
    (summary/report results)
    (summarize results)))

(defn run-all-tests
  "Run tests defined in all namespaces."
  []
  (let [results (runner/run-all-tests)]
    (summary/report results)
    (summarize results)))

(mx/defn run-test-var
  [v :- :lt/var]
  (let [results (runner/run-test-var v)]
    (summary/report results)
    (summarize results)))
