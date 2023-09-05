(ns lazytest.runner.repl
  (:require
   [lazytest.report.summary :as summary]
   [lazytest.runner :as runner]
   [lazytest.results :refer [summarize]]))

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

(defn run-test-var
  [v]
  {:pre [(var? v)]}
  (let [results (runner/run-test-var v)]
    (summary/report results)
    (summarize results)))
