(ns lazytest.reporters 
  (:require
   [lazytest.color :refer [colorize]]
   [lazytest.suite :as s :refer [suite-result?]]
   [lazytest.test-case :as tc]
   [clojure.string :as str]
   [clojure.stacktrace :as stack]
   [clojure.data :refer [diff]]
   [clojure.pprint :as pp]))

(defn combine-reporters
  ([reporter] (fn [context m] (reporter context m) (flush) nil))
  ([reporter & reporters]
   (fn [context m]
     (run! (fn [reporter] (reporter context m) (flush) nil)
           (cons reporter reporters)))))

(defn report [context m]
  (when-let [reporter (:reporter context)]
    (reporter context m)))

(defn test-case-str [result]
  (or (:doc result) "Anonymous test case"))

(defn identifier [m]
  (or (:doc m) (:ns-name m) (some-> m :var symbol str)))

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

;; RESULTS
;; Print the failed assertions, their arguments, etc.
;;
;; Example:

(defmacro pprint-out [obj]
  `(str/trim (with-out-str (pp/pprint ~obj))))

(defn- print-equality-failed [a b]
  (let [[a b same] (diff a b)]
    (println (colorize "Only in first argument:" :cyan))
    (pp/pprint a)
    (println (colorize "Only in second argument:" :cyan))
    (pp/pprint b)
    (when (some? same)
      (println (colorize "The same in both:" :cyan))
      (pp/pprint same))))

(defn- print-evaluated-arguments [reason]
  (println (colorize "Evaluated arguments:" :cyan))
  (doseq [arg (rest (:evaluated reason))]
    (print "* ")
    (if (instance? Throwable arg)
      (pp/pprint (type arg))
      (pp/pprint arg))))

(defn- report-test-case-failure [result]
  (let [docs (conj (:docs result) (test-case-str result))
        report-type (:type result)
        docstring (format
                   "%s: %s"
                   (if (= :fail report-type) "FAILURE" "ERROR")
                   (str/join " " (remove nil? docs)))
        error (:thrown result)
        reason (ex-data error)]
    (newline)
    (println (colorize docstring :red))
    (printf "in %s:%s\n" (:file result) (:line result))
    (if (= :fail report-type)
      (do (println (colorize "Expression:" :cyan)
                   (pprint-out (:expected reason)))
        (println (colorize "Result:" :cyan)
                 (pprint-out (:actual reason)))
        (when (:evaluated reason)
          (print-evaluated-arguments reason)
          (when (and (= '= (first (:evaluated reason)))
                     (= 3 (count (:evaluated reason))))
            (apply print-equality-failed (rest (:evaluated reason))))))
      (stack/print-cause-trace error))))

(defmulti ^:private results-builder (fn [m] (:type (meta m))))

(defmethod results-builder ::s/suite-result
  [{:keys [docs children] :as results
    :or {docs []}}]
  (doseq [child children
          :let [m (meta (:source results))
                docs (conj docs (identifier m))
                child (assoc child :docs docs)]]
    (results-builder child)))

(defmethod results-builder ::tc/test-case-result
  [result]
  (when-not (= :pass (:type result))
    (report-test-case-failure result)))

(defmulti results #'reporter-dispatch)
(defmethod results :default [_ _])
(defmethod results :end-test-run [_ m]
  (results-builder (:results m)))

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
  [focused dots* results summary])

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
  (let [id (identifier s)
        depth (:lazytest.runner/depth context 0)]
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
  (let [id (test-case-str result)]
    (indent (:depth context 0))
    (let [result-type (:type result)
          msg (str id (when (not= :pass result-type)
                        (str " " (str/upper-case (name result-type)))))]
      (println (colorize msg
                         (if (= :pass result-type) :green :red))))))

(defmethod nested* :pass [context result] (print-test-result context result))
(defmethod nested* :fail [context result] (print-test-result context result))
(defmethod nested* :error [context result] (print-test-result context result))

(def nested
  [focused nested* summary])

;; CLOJURE-TEST
;; Attempts to mirror clojure.test's default reporter
;;
;; Testing lazytest.core-test
;;
;; FAIL in (with-redefs-test) (lazytest/core_test.clj:28)
;; Namespaces lazytest.core-test #'lazytest.core-test/with-redefs-test
;; expected: (= 7 (plus 2 3))
;;   actual: false

(defmulti clojure-test #'reporter-dispatch)
(defmethod clojure-test :default [_ _])

(defn- clojure-test-case-str [context result]
  (format "%s (%s:%s)"
          (str (apply list (map #(:name (meta %)) (:lazytest.runner/current-var context))))
          (or (:file result) "Unknown file")
          (:line result)))

(defmethod clojure-test :fail [context result]
  (println "\nFAIL in" (clojure-test-case-str context result))
  (when-let [strings (seq (:lazytest.runner/testing-strings context))]
    (println (str/join " " strings)))
  (when-let [message (:doc result)]
    (println message))
  (println "expected:" (pr-str (:expected result)))
  (println "  actual:" (pr-str (:actual result))))

(defmethod clojure-test :error [context result]
  (println "\nERROR in" (clojure-test-case-str context result))
  (when-let [strings (seq (:testing-strings context))]
    (println (str/join " " strings)))
  (when-let [message (:doc result)]
    (println message))
  (println "expected:" (pr-str (:expected result)))
  (print "  actual: ")
  (let [actual (:thrown result)]
    (if (instance? Throwable actual)
      (stack/print-cause-trace actual nil)
      (prn actual)))
  (flush))

(defmethod clojure-test :begin-ns-suite [_ result]
  (println "\nTesting" (ns-name (:ns-name result))))

(defmethod clojure-test :end-test-run [_ m]
  (let [results (:results m)
        test-vars (count (filter #(= :lazytest/test-var (type (:source %))) (result-seq results)))
        test-case-results (remove suite-result? (result-seq results))
        total (count test-case-results)
        {:keys [fail error]} (group-by :type test-case-results)
        fails (count fail)
        errors (count error)]
    (newline)
    (println (format "Ran %s tests containing %s test cases." test-vars total))
    (println (format "%s %s, %s %s."
                     fails 
                     (str "failure" (when (not= 1 fails) "s"))
                     errors
                     (str "error" (when (not= 1 errors) "s"))))
    (flush)))
