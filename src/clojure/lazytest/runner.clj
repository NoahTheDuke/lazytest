(ns lazytest.runner
  (:require
   [lazytest.find :refer [find-suite]]
   [lazytest.focus :refer [filter-tree focused?]]
   [lazytest.malli]
   [lazytest.reporters :refer [dots report]]
   [lazytest.suite :refer [expand-tree suite-result test-seq]]
   [lazytest.test-case :refer [try-test-case]]
   [malli.experimental :as mx]))

(defn dispatch [_context m]
  (or (:type m) (-> m meta :type)))

(defmulti run-test #'dispatch)
(defmethod run-test :default [_context m]
  (throw (ex-info "Non-test given to run-suite." {:obj m})))
(defmethod run-test nil [_context _])

(defn ->suite-result [context s]
  (let [sm (meta s)
        id (or (:doc sm) (:ns-name sm) (:var sm))
        context (if id (update context :depth (fnil inc 0)) context)]
    (suite-result s (vec (keep #(run-test context %) s)))))

(defmethod run-test :lazytest/run [context s]
  (let [sm (meta s)]
    (report context (assoc sm :type :begin-test-run))
    (let [results (->suite-result context s)]
      (report context (assoc sm :type :end-test-run :results results))
      results)))

(defmethod run-test :lazytest/ns-suite [context s]
  (let [sm (meta s)]
    (report context (assoc sm :type :begin-ns-suite))
    (let [results (->suite-result context s)]
      (report context (assoc sm :type :end-ns-suite :results results))
      results)))

(defmethod run-test :lazytest/test-var [context s]
  (let [sm (meta s)]
    (report context (assoc sm :type :begin-test-var))
    (let [results (->suite-result context s)]
      (report context (assoc sm :type :end-test-var :results results))
      results)))

(defmethod run-test :lazytest/suite [context s]
  (let [sm (meta s)]
    (report context (assoc sm :type :begin-test-suite))
    (let [results (->suite-result context s)]
      (report context (assoc sm :type :end-test-suite :results results))
      results)))

(defmethod run-test :lazytest/test-seq [context s]
  (let [sm (meta s)]
    (report context (assoc sm :type :begin-test-seq))
    (let [results (->suite-result context s)]
      (report context (assoc sm :type :end-test-seq :results results))
      results)))

(defmethod run-test :lazytest/test-case [context tc]
  (let [tc-meta (meta tc)]
    (report context (assoc tc-meta :type :begin-test-case))
    (let [results (try-test-case tc)]
      (report context results)
      (report context (assoc tc-meta :type :end-test-case :results results))
      results)))

(defn run-tests
  "Runs tests defined in the given namespaces."
  ([namespaces] (run-tests {:reporter dots} namespaces))
  ([context namespaces]
   (let [ste (apply find-suite namespaces)
         tree (filter-tree (expand-tree ste))
         result (run-test context tree)]
     (if (focused? tree)
       (vary-meta result assoc :focus true)
       result))))

(defn run-all-tests
  "Run tests defined in all namespaces."
  []
  (run-tests nil))

(mx/defn run-test-var
  [v :- [:fn var?]]
  (let [tree (-> (vary-meta @v assoc :lazytest.suite/suite true)
                 (expand-tree)
                 (test-seq))]
    (run-test tree)))
