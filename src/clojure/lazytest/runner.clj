(ns lazytest.runner
  (:require
   [lazytest.find :refer [find-suite find-var-test-value]]
   [lazytest.filter :refer [filter-tree]]
   [lazytest.malli]
   [lazytest.reporters :as r :refer [nested report]]
   [lazytest.suite :as s :refer [expand-tree suite-result]]
   [lazytest.test-case :refer [try-test-case]]
   [malli.experimental :as mx]))

(set! *warn-on-reflection* true)

(defn dispatch [_config m]
  (or (:type m) (-> m meta :type)))

(defmulti run-test #'dispatch)
(defmethod run-test :default [_config m]
  (throw (ex-info "Non-test given to run-suite." {:obj m})))
(defmethod run-test nil [_config _])

(defn ->suite-result [config s source-type]
  (let [sm (meta s)
        id (s/identifier sm)
        start (System/nanoTime)
        config (-> config
                    (update ::depth #(if id (inc %) %))
                    (update ::suite-history conj sm))
        results (vec (keep #(run-test config %) s))
        duration (double (- (System/nanoTime) start))]
    (-> (suite-result s results)
        (assoc ::source-type source-type)
        (assoc ::duration duration))))

(defmethod run-test :lazytest/run [config s]
  (let [sm (meta s)]
    (report config (assoc sm :type :begin-test-run))
    (let [results (->suite-result config s :lazytest/run)]
      (report config (assoc sm :type :end-test-run :results results))
      results)))

(defmethod run-test :lazytest/ns-suite [config s]
  (let [sm (meta s)]
    (report config (assoc sm :type :begin-ns-suite))
    (let [results (->suite-result config s :lazytest/ns-suite)]
      (report config (assoc sm :type :end-ns-suite :results results))
      results)))

(defmethod run-test :lazytest/test-var [config s]
  (let [sm (meta s)]
    (report config (assoc sm :type :begin-test-var))
    (let [results (->suite-result config s :lazytest/test-var)]
      (report config (assoc sm :type :end-test-var :results results))
      results)))

(defmethod run-test :lazytest/suite [config s]
  (let [sm (meta s)]
    (report config (assoc sm :type :begin-test-suite))
    (let [results (->suite-result config s :lazytest/suite)]
      (report config (assoc sm :type :end-test-suite :results results))
      results)))

(defmethod run-test :lazytest/test-seq [config s]
  (let [sm (meta s)]
    (report config (assoc sm :type :begin-test-seq))
    (let [results (->suite-result config s :lazytest/test-seq)]
      (report config (assoc sm :type :end-test-seq :results results))
      results)))

(defmethod run-test :lazytest/test-case [config tc]
  (let [tc-meta (meta tc)
        start (System/nanoTime)]
    (report config (assoc tc-meta :type :begin-test-case))
    (let [results (try-test-case tc)
          duration (double (- (System/nanoTime) start))
          results (assoc results ::duration duration)]
      (report config results)
      (report config (assoc tc-meta :type :end-test-case :results results))
      results)))

(defn run-tests
  "Runs tests defined in the given namespaces."
  ([namespaces] (run-tests {:reporter nested} namespaces))
  ([config namespaces]
   (let [ste (apply find-suite namespaces)
         tree (filter-tree config (expand-tree ste))
         result (run-test config tree)]
     (if (:focus (meta tree))
       (vary-meta result assoc :focus true)
       result))))

(defn run-all-tests
  "Run tests defined in all namespaces."
  ([] (run-all-tests nil))
  ([config]
   (run-tests config nil)))

(mx/defn run-test-var
  [config v :- [:fn var?]]
  (let [tree (-> (find-var-test-value v)
                 (vary-meta assoc :type :lazytest/run)
                 (expand-tree)
                 (#(filter-tree config %)))]
    (run-test config tree)))
