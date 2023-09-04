(ns lazytest.report.console
  (:require
   [lazytest.color :refer [colorize]]
   [lazytest.focus :refer [focused?]]
   [lazytest.suite :as s]
   [lazytest.test-case :as tc]))

(defn- dispatch [result] (:type (meta result)))

(defmulti console #'dispatch)

(defmethod console ::s/suite-result
  [result]
  (run! console (:children result))
  (flush))

(defmethod console ::tc/test-case-result
  [result]
  (case (:type result)
    :pass (print (colorize "." :green))
    :fail (print (colorize "F" :red))
    :error (print (colorize "E" :red))
    nil))

(defn report
  "Print test results, with colored green dots indicating passing tests and red 'F's
  indicating falied tests."
  [results]
  (when (focused? results)
    (println "=== FOCUSED TESTS ONLY ==="))
  (console results)
  (newline)
  results)
