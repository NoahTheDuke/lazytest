(ns lazytest.reporters 
  (:require
   [lazytest.color :refer [colorize]]
   [lazytest.suite :as s :refer [suite-result?]]
   [clojure.string :as str]))

(defn combine-reporters
  ([reporter] (fn [context m] (reporter context m) (flush) nil))
  ([reporter & reporters]
   (fn [context m]
     (run! (fn [reporter] (reporter context m) (flush) nil)
           (cons reporter reporters)))))

(defn report [context m]
  (when-let [reporter (:reporter context)]
    (reporter context m)))

(defn reporter-dispatch [_context m] (:type m))

;; FOCUSED
;; Prints a message when tests are focused. Included by default.

(defmulti focused #'reporter-dispatch)
(defmethod focused :default [_ _])
(defmethod focused :begin-test-run [_ m]
  (when (:focus m)
    (println "=== FOCUSED TESTS ONLY ==="))
  (flush))

;; SUMMARY
;; Prints the number of test cases, failures, and errors.
;;
;; Example:
;; Ran 5 test cases.
;; 0 failures and 2 errors.

(defn result-seq
  "Given a single suite result, returns a depth-first sequence of all
  nested child suite/test results."
  [result]
  (tree-seq suite-result? :children result))

(defn summarize
  "Given a sequence of suite results, returns a map of counts with
  keys :total, :pass, and :fail."
  [results]
  (let [test-case-results (remove suite-result? (result-seq results))
        total (count test-case-results)
        {:keys [pass fail error]} (group-by :type test-case-results)]
    {:total total
     :pass (count pass)
     :fail (count fail)
     :error (count error)
     :not-passing (+ (count fail) (count error))}))

(defmulti summary #'reporter-dispatch)
(defmethod summary :default [_ _])
(defmethod summary :end-test-run [_ m]
  (let [{:keys [total fail error not-passing]} (summarize (:results m))
        count-msg (str "Ran " total " test cases.")]
    (newline)
    (println (if (zero? total)
               (colorize count-msg :yellow)
               count-msg))
    (println (colorize (str fail " failure" (when (not= 1 fail) "s")
                            (if (pos? error)
                              (format " and %s errors." error)
                              "."))
                       (if (zero? not-passing) :green :red)))
    (flush)))

;; DOTS
;; Passing test cases are printed as ., failures as F, and errors as E.
;; Test cases in namespaces are wrapped in parentheses.
;;
;; Example:
;; (....)(.)(....)

(defmulti dots* #'reporter-dispatch)
(defmethod dots* :default [_ _])
(defmethod dots* :pass [_ _] (print (colorize "." :green)))
(defmethod dots* :fail [_ _] (print (colorize "F" :red)))
(defmethod dots* :error [_ _] (print (colorize "E" :red)))
(defmethod dots* :begin-ns-suite [_ _] (print (colorize "(" :yellow)))
(defmethod dots* :end-ns-suite [_ _] (print (colorize ")" :yellow)))
(defmethod dots* :end-test-run [_ _] (newline))

(def dots
  [focused dots* summary])

;; NESTED
;; Print each suite and test case on a new line, and indent each suite.
;;
;; Example:
;; Namespaces
;;   lazytest.readme-test
;;     The square root of two
;;       is less than two
;;       is more than one

(defn- indent [n]
  (print (apply str (repeat n "  "))))

(defmulti nested* #'reporter-dispatch)
(defmethod nested* :default [_ _])

(defn print-test-seq
  [context s]
  (let [id (or (:doc s) (:ns-name s) (some-> s :var symbol str))
        depth (:depth context 0)]
    (when id
      (indent depth)
      (println id))))

(defmethod nested* :begin-test-run [context s] (print-test-seq context s))
(defmethod nested* :begin-ns-suite [context s] (print-test-seq context s))
(defmethod nested* :begin-test-var [context s] (print-test-seq context s))
(defmethod nested* :begin-test-suite [context s] (print-test-seq context s))
(defmethod nested* :begin-test-seq [context s] (print-test-seq context s))

(defn print-test-result
  [context result]
  (let [m (meta (:source result))]
    (when-let [id (or (:doc m) (:ns-name m))]
      (indent (:depth context 0))
      (let [result-type (:type result)
            msg (str id (when (not= :pass result-type)
                         (str " " (str/upper-case (name result-type)))))]
        (println (colorize msg
                           (if (= :pass result-type) :green :red)))))))

(defmethod nested* :pass [context result] (print-test-result context result))
(defmethod nested* :fail [context result] (print-test-result context result))
(defmethod nested* :error [context result] (print-test-result context result))

(def nested
  [focused nested* summary])
