(ns lazytest.report.nested
  "Nested doc printout:

  Namespaces
    lazytest.readme-test
      The square root of two
        <green>is less than two
        <green>is more than one
  ...

  "
  (:require
   [lazytest.color :refer [colorize]]
   [lazytest.suite :refer [suite-result?]]))

(set! *warn-on-reflection* true)

(defn- identifier [result]
  (let [m (meta (:source result))]
    (or (:doc m) (:ns-name m))))

;;; Nested doc printout

(declare report-docs)

(defn- indent [n]
  (print (apply str (repeat n "  "))))

(defn- report-suite-result [result depth]
  (let [id (identifier result)]
    (when id
      (indent depth)
      (println id))
    (let [depth (if id (inc depth) depth)]
      (doseq [child (:children result)]
        (report-docs child depth)))))

(defn- report-test-case-result [result depth]
  (indent depth)
  (println (colorize (str (identifier result))
                     (if (= :pass (:type result)) :green :red))))

(defn- report-docs [result depth]
  (if (suite-result? result)
    (report-suite-result result depth)
    (report-test-case-result result depth)))

;;; Entry point

(defn report [results]
  (report-docs results 0)
  (flush)
  results)
