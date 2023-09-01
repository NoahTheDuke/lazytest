(ns lazytest.runner.console
  (:require
    [clojure.test]
    [lazytest.color :refer [colorize]]
    [lazytest.find :refer [find-suite]]
    [lazytest.focus :refer [filter-tree focused?]]
    [lazytest.suite :refer [expand-tree suite-result test-seq? test-seq suite]]
    [lazytest.test-case :refer [test-case? try-test-case]]))

(defn- run-test-case [tc]
  (let [result (try-test-case tc)]
    (if (:pass? result)
      (print (colorize "." :green))
      (print (colorize "F" :red)))
    (flush)
    result))

(defn- run-test-seq [s]
  (let [results
        (mapv (fn [x]
                (cond
                  (test-seq? x) (run-test-seq x)
                  (test-case? x) (run-test-case x)
                  :else (throw (IllegalArgumentException.
                                 "Non-test given to run-suite."))))
              s)]
    (suite-result s results)))

(defn run-tests
  "Runs tests defined in the given namespaces, with colored green dots
  indicating passing tests and red 'F's indicating falied tests."
  [& namespaces]
  (let [ste (apply find-suite namespaces)
        tree (filter-tree (expand-tree ste))]
    (when (focused? tree)
      (println "=== FOCUSED TESTS ONLY ==="))
    (let [result (run-test-seq tree)]
      (newline)
      result)))

(defn run-test-var
  [v]
  (let [tree (-> (suite @v)
                 (expand-tree)
                 (test-seq))
        result (run-test-seq tree)]
    (newline)
    result))

(defn run-all-tests
  "Run tests defined in all namespaces."
  []
  (run-tests))
