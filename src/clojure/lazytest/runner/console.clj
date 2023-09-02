(ns lazytest.runner.console
  (:require
    [lazytest.color :refer [colorize]]
    [lazytest.focus :refer [focused?]]
    [lazytest.runner :as runner]
    [lazytest.suite :refer [suite-result?]]))

(declare report-result)

(defn- report-test-case-result [result]
  (condp = (:type result)
    :pass (print (colorize "." :green))
    :fail (print (colorize "F" :red))
    :error (print (colorize "E" :red))))

(defn- report-suite-result [result]
  (run! report-result (:children result))
  (flush))

(defn- report-result [result]
  (if (suite-result? result)
    (report-suite-result result)
    (report-test-case-result result)))

(defn run-tests
  "Runs tests defined in the given namespaces, with colored green dots
  indicating passing tests and red 'F's indicating falied tests."
  [& namespaces]
  (let [result (apply runner/run-tests namespaces)]
    (when (focused? result)
      (println "=== FOCUSED TESTS ONLY ==="))
    (report-result result)
    (newline)
    result))

(defn run-all-tests
  "Run tests defined in all namespaces."
  []
  (run-tests))

(defn run-test-var
  [v]
  (let [result (runner/run-test-var v)]
    (report-result result)
    (newline)
    result))
