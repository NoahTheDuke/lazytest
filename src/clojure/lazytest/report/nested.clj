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
    [clojure.data :refer [diff]]
    [clojure.pprint :refer [pprint]]
    [clojure.stacktrace :refer [print-cause-trace]]
    [lazytest.color :refer [colorize]]
    [lazytest.results :refer [summarize]]
    [lazytest.suite :refer [suite-result?]]
    [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn- identifier [result]
  (let [m (meta (:source result))]
    (or (:doc m) (:name m))))

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

;;; Failures

(defn- print-equality-failed [a b]
  (let [[a b same] (diff a b)]
    (println (colorize "Only in first argument:" :cyan))
    (pprint a)
    (println (colorize "Only in second argument:" :cyan))
    (pprint b)
    (when (some? same)
      (println (colorize "The same in both:" :cyan))
      (pprint same))))

(defn- print-evaluated-arguments [reason]
  (println (colorize "Evaluated arguments:" :cyan))
  (doseq [arg (rest (:evaluated reason))]
    (print "* ")
    (pprint arg)))

(defn- print-expectation-failed [err]
  (let [reason (ex-data err)]
    (println "at" (:file reason) "line" (:line reason))
    (println (colorize "Expression:" :cyan))
    (pprint (:form reason))
    (println (colorize "Result:" :cyan))
    (pprint (:result reason))
    (when (:evaluated reason)
      (print-evaluated-arguments reason)
      (when (and (= = (first (:evaluated reason)))
                 (= 3 (count (:evaluated reason))))
        (apply print-equality-failed (rest (:evaluated reason)))))))

(defn- report-test-case-failure [result docs]
  (when-not (= :pass (:type result))
    (let [docs (conj docs (identifier result))
          report-type (:type result)
          docstring (format
                     "%s: %s"
                     (if (= :fail report-type) "FAILURE" "ERROR")
                     (str/join " " (remove nil? docs)))
          error (:thrown result)]
      (println (colorize docstring :red))
      (if (= :fail report-type)
        (print-expectation-failed error)
        (print-cause-trace error))
      (newline))))

(defn- report-failures [result docs]
  (if (suite-result? result)
    (doseq [child (:children result)]
      (report-failures child (conj docs (identifier result))))
    (report-test-case-failure result docs)))

;;; Summary

(defn report-summary [summary]
  (let [{:keys [total not-passing]} summary
        count-msg (str "Ran " total " test cases.")]
    (println (if (zero? total)
               (colorize count-msg :yellow)
               count-msg))
    (println (colorize (str not-passing " failures.")
               (if (zero? not-passing) :green :red)))))

;;; Entry point

(defn report [result]
  (report-docs result 0)
  (newline)
  (report-failures result [])
  (report-summary (summarize [result])))
