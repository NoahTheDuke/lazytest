(ns lazytest.runner
  (:require
    [lazytest.find :refer [find-suite]]
    [lazytest.focus :refer [filter-tree focused?]]
    [lazytest.suite :refer [expand-tree suite-result test-seq? test-seq suite]]
    [lazytest.test-case :refer [test-case? try-test-case]]))

(defn- run-test-seq [s]
  (let [results
        (mapv (fn [x]
                (cond
                  (test-seq? x) (run-test-seq x)
                  (test-case? x) (try-test-case x)
                  :else (throw (IllegalArgumentException.
                                 "Non-test given to run-suite."))))
              s)]
    (suite-result s results)))

(defn run-tests
  "Runs tests defined in the given namespaces."
  [& namespaces]
  (let [ste (apply find-suite namespaces)
        tree (filter-tree (expand-tree ste))
        result (run-test-seq tree)]
    (if (focused? tree)
      (vary-meta result assoc :focus true)
      result)))

(defn run-all-tests
  "Run tests defined in all namespaces."
  []
  (run-tests))

(defn run-test-var
  [v]
  {:pre [(var? v)]}
  (let [tree (-> (suite @v)
                 (expand-tree)
                 (test-seq))]
    (run-test-seq tree)))
