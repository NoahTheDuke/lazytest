(ns lazytest.report.console
  (:require
    [lazytest.color :refer [colorize]]
    [lazytest.focus :refer [focused?]]
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

(defn report
  "Print test results, with colored green dots indicating passing tests and red 'F's
  indicating falied tests."
  [results]
  (when (focused? results)
    (println "=== FOCUSED TESTS ONLY ==="))
  (report-result results)
  (newline)
  results)
