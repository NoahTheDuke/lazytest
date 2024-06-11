(ns lazytest.runner
  (:require
   [lazytest.find :refer [find-suite]]
   [lazytest.focus :refer [filter-tree focused?]]
   [lazytest.suite :refer [expand-tree suite-result test-seq]]
   [lazytest.test-case :refer [try-test-case]]
   [malli.experimental :as mx]
   [lazytest.color :refer [colorize]]))

(defn report-dispatch [m] (:type m))

(defmulti report #'report-dispatch)

(defmethod report :default [_])
(defmethod report :pass [_] (print (colorize "." :green)))
(defmethod report :fail [_] (print (colorize "F" :red)))
(defmethod report :error [_] (print (colorize "E" :red)))
(defmethod report :begin-ns-suite [_] (print (colorize "(" :yellow)))
(defmethod report :end-ns-suite [_] (print (colorize ")" :yellow)))
(defmethod report :end-test-run [_] (newline))

(defn dispatch [m]
  (or (:type m) (type m)))

(defmulti run-test #'dispatch)
(defmethod run-test :default [m]
  (throw (ex-info "Non-test given to run-suite." {:obj m})))
(defmethod run-test nil [_])

(defn ->suite-result [s]
  (suite-result s (vec (keep run-test s))))

(defmethod run-test :lazytest/run [s]
  (let [s-meta (meta s)]
    (report {:type :begin-test-run
             :doc (:doc s-meta)
             :nses (:nses s-meta)})
    (let [results (->suite-result s)]
      (report {:type :end-test-run
               :doc (:doc s-meta)
               :nses (:nses s-meta)
               :results results})
      results)))

(defmethod run-test :lazytest/ns-suite [s]
  (let [s-meta (meta s)]
    (report {:type :begin-ns-suite
             :ns-name (:ns-name s-meta)})
    (let [results (->suite-result s)]
      (report {:type :end-ns-suite
               :ns-name (:ns-name s-meta)
               :results results})
      results)))

(defmethod run-test :lazytest/test-var [s]
  (let [s-meta (meta s)]
    (report {:type :begin-test-var
             :doc (:doc s-meta)
             :var (:var s-meta)})
    (let [results (->suite-result s)]
      (report {:type :end-test-var
               :doc (:doc s-meta)
               :var (:var s-meta)
               :results results})
      results)))

(defmethod run-test :lazytest/suite [s]
  (let [s-meta (meta s)]
    (report {:type :begin-test-suite
             :doc (:doc s-meta)})
    (let [results (->suite-result s)]
      (report {:type :end-test-suite
               :doc (:doc s-meta)
               :results results})
      results)))

(defmethod run-test :lazytest/test-seq [s]
  (let [s-meta (meta s)]
    (report {:type :begin-test-seq
             :doc (:doc s-meta)})
    (let [results (->suite-result s)]
      (report {:type :end-test-seq
               :doc (:doc s-meta)
               :results results})
      results)))

(defmethod run-test :lazytest/test-case [tc]
  (let [tc-meta (meta tc)]
    (report {:type :begin-test-case
             :doc (:doc tc-meta)})
    (let [results (try-test-case tc)]
      (report results)
      (report {:type :end-test-case
               :doc (:doc tc-meta)
               :results results})
      results)))

(defmethod run-test :lazytest.suite/suite-result [_])
(defmethod run-test :lazytest.test-case/test-case-result [_])

(defn run-tests
  "Runs tests defined in the given namespaces."
  [& namespaces]
  (let [ste (apply find-suite namespaces)
        tree (filter-tree (expand-tree ste))
        result (run-test tree)]
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
    (run-test tree)))
