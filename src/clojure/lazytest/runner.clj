(ns lazytest.runner
  (:require
    [lazytest.find :refer [find-suite]]
    [lazytest.focus :refer [filter-tree focused?]]
    [lazytest.suite :refer [expand-tree suite-result test-seq? test-seq]]
    [lazytest.test-case :refer [test-case? try-test-case]]
    [malli.experimental :as mx]))

(defn dispatch [m] (-> m meta :type))
(defmulti run-hook #'dispatch)

(defmethod run-hook :default [_])
(defmethod run-hook :lazytest/run [_])
(defmethod run-hook :lazytest/suite [_])
(defmethod run-hook :lazytest/ns-suite [_])
(defmethod run-hook :lazytest/test-seq [_])
(defmethod run-hook :lazytest/test-var [_])
(defmethod run-hook :lazytest/test-case [_])

(defn- run-test-seq [s]
  (let [results
        (mapv (fn [x]
                (run-hook x)
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

(mx/defn run-test-var
  [v :- [:fn var?]]
  (let [tree (-> (vary-meta @v assoc :lazytest.suite/suite true)
                 (expand-tree)
                 (test-seq))]
    (run-test-seq tree)))
