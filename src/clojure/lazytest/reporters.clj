(ns lazytest.reporters
  (:require
   [clojure.data :refer [diff]]
   [clojure.pprint :as pp]
   [clojure.stacktrace :as stack]
   [clojure.string :as str]
   [lazytest.color :refer [colorize]]
   [lazytest.results :refer [result-seq summarize]]
   [lazytest.suite :as s :refer [suite-result?]]
   [lazytest.test-case :as tc]))

(defn report [context m]
  (when-let [reporter (:reporter context)]
    (reporter context m)))

(defn reporter-dispatch [_context m] (:type m))

(defn- indent [n]
  (print (apply str (repeat n "  "))))

;; FOCUSED
;; Prints a message when tests are focused. Included by default.
;;
;; Example:
;;
;; === FOCUSED TESTS ONLY ===

(defmulti focused {:arglists '([context m])} #'reporter-dispatch)
(defmethod focused :default [_ _])
(defmethod focused :begin-test-run [_ m]
  (when (:focus m)
    (println "=== FOCUSED TESTS ONLY ===")
    (newline))
  (flush))

;; SUMMARY
;; Prints the number of test cases, failures, and errors.
;;
;; Example:
;;
;; Ran 5 test cases.
;; 0 failures and 2 errors.

(defmulti summary {:arglists '([context m])} #'reporter-dispatch)
(defmethod summary :default [_ _])
(defmethod summary :end-test-run [_ m]
  (let [{:keys [total fail error not-passing]} (summarize (:results m))
        duration (double (/ (-> m :results (:lazytest.runner/duration 0.0)) 1e9))
        count-msg (format "Ran %s test cases in %.5f seconds." total duration)]
    (println (if (zero? total)
               (colorize count-msg :yellow)
               count-msg))
    (println (colorize (str fail " failure" (when (not= 1 fail) "s")
                            (if (pos? error)
                              (format " and %s errors." error)
                              "."))
                       (if (zero? not-passing) :green :red)))
    (newline)
    (flush)))

;; RESULTS
;; Print the failed assertions, their arguments, etc.
;;
;; Example:
;;
;; lazytest.core-test
;;   with-redefs-test
;;     redefs inside 'it' blocks:
;;
;; this should be true
;; Expected: (= 7 (plus 2 3))
;; Actual: false
;; Evaluated arguments:
;;  * 7
;;  * 6
;; Only in first argument:
;; 7
;; Only in second argument:
;; 6
;;
;; in lazytest/core_test.clj:30

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

(defn- print-evaluated-arguments [result]
  (println (colorize "Evaluated arguments:" :cyan))
  (doseq [arg (rest (:evaluated result))]
    (print " * ")
    (if (instance? Throwable arg)
      (pp/pprint (type arg))
      (pp/pprint arg))))

(defn- print-context [docs]
  (loop [[doc & docs] (seq (filter identity docs))
         idx 0]
    (when doc
      (indent idx)
      (if docs
        (println doc)
        (println (str doc ":")))
      (recur docs (inc idx)))))

(defn- report-test-case-failure [result]
  (print-context (:docs result))
  (let [report-type (:type result)
        message (or (:message result)
                    (if (= :fail report-type)
                      "Expectation failed"
                      "ERROR: Caught exception"))]
    (newline)
    (println (colorize message :red))
    (if (= :fail report-type)
      (do (println (colorize "Expected:" :cyan)
                   (pr-str (:expected result)))
        (println (colorize "Actual:" :cyan)
                 (pr-str (:actual result)))
        (when (:evaluated result)
          (print-evaluated-arguments result)
          (when (and (= = (first (:evaluated result)))
                     (= 3 (count (:evaluated result))))
            (apply print-equality-failed (rest (:evaluated result))))))
      (stack/print-cause-trace (:actual result) 10))
    (newline)
    (println (colorize (format "in %s:%s\n" (:file result) (:line result)) :light)))
  (flush))

(defmulti ^:private results-builder {:arglists '([context m])} #'reporter-dispatch)
(defmethod results-builder :pass [_ _])

(defmethod results-builder ::s/suite-result
  [context {:keys [children] :as results}]
  (doseq [child children
          :let [docs (conj (:docs results []) (s/identifier results))
                child (assoc child :docs docs)]]
    (results-builder context child)))

(defmethod results-builder :fail [_context result]
  (-> result
      (update :docs conj (tc/identifier result))
      (report-test-case-failure)))
(defmethod results-builder :error [_context result]
  (-> result
      (update :docs conj (tc/identifier result))
      (report-test-case-failure)))

(defmulti results {:arglists '([context m])} #'reporter-dispatch)
(defmethod results :default [_ _])
(defmethod results :end-test-run [context m]
  (results-builder context (:results m)))

;; DOTS
;; Passing test cases are printed as ., failures as F, and errors as E.
;; Test cases in namespaces are wrapped in parentheses.
;;
;; Example:
;;
;; (....)(.)(....)

(defmulti dots* {:arglists '([context m])} #'reporter-dispatch)
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
;;
;;   lazytest.readme-test
;;     The square root of two
;;       is less than two
;;       is more than one

(defmulti nested* {:arglists '([context m])} #'reporter-dispatch)
(defmethod nested* :default [_ _])

(defn print-test-seq
  [context s]
  (let [id (s/identifier s)
        depth (:lazytest.runner/depth context)]
    (when id
      (indent depth)
      (println id))))

(defmethod nested* :begin-test-run [context s] (print-test-seq context s))
(defmethod nested* :begin-ns-suite [context s] (print-test-seq context s))
(defmethod nested* :begin-test-var [context s] (print-test-seq context s))
(defmethod nested* :begin-test-suite [context s] (print-test-seq context s))
(defmethod nested* :begin-test-seq [context s] (print-test-seq context s))
(defmethod nested* :end-test-run [_ _] (newline) (flush))

(defn print-test-result
  [context result]
  (let [id (tc/identifier result)]
    (indent (:lazytest.runner/depth context))
    (let [result-type (:type result)
          msg (str id (when (not= :pass result-type)
                        (str " " (str/upper-case (name result-type)))))]
      (case result-type
        :pass (println (colorize "√" :green) (colorize msg :light))
        :pending (println (colorize "-" :cyan) (colorize msg :cyan))
        (:fail :error) (println (colorize "×" :red) (colorize msg :red))
        #_:else nil))))

(defmethod nested* :pass [context result] (print-test-result context result))
(defmethod nested* :fail [context result] (print-test-result context result))
(defmethod nested* :error [context result] (print-test-result context result))

(def nested
  [focused nested* results summary])

;; CLOJURE-TEST
;; Adapts clojure.test's default reporter to Lazytests' system.
;; It treats suite :docs as context strings and 
;;
;; Example:
;;
;; Testing lazytest.core-test
;;
;; FAIL in (with-redefs-test) (lazytest/core_test.clj:33)
;; redefs outside 'it' blocks should not be rebound
;; expected: (not= 5 (plus 2 3))
;;   actual: false
;;
;; Ran 12 tests containing 29 test cases.
;; 1 failure, 0 errors.

(defmulti clojure-test {:arglists '([context m])} #'reporter-dispatch)
(defmethod clojure-test :default [_ _])

(defn- clojure-test-case-str [context result]
  (format "(%s) (%s:%s)"
          (->> (:lazytest.runner/suite-history context)
               (keep :var)
               (map #(:name (meta %)))
               (str/join " "))
          (:file result)
          (:line result)))

(defmethod clojure-test :fail [context result]
  (println "\nFAIL in" (clojure-test-case-str context result))
  (when-let [strings (->> (conj (:lazytest.runner/suite-history context) result)
                          (keep :doc)
                          (seq))]
    (println (str/join " " strings)))
  (when-let [message (:message result)]
    (println message))
  (println "expected:" (pr-str (:expected result)))
  (println "  actual:" (pr-str (:actual result))))

(defmethod clojure-test :error [context result]
  (println "\nERROR in" (clojure-test-case-str context result))
  (when-let [strings (->> (conj (:lazytest.runner/suite-history context) result)
                          (keep :doc)
                          (seq))]
    (println (str/join " " strings)))
  (when-let [message (:message result)]
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
        test-vars (count (filter #(= :lazytest/test-var (type (:source %)))
                                 (result-seq results)))
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

;; VERBOSE
;; Prints loudly about every step of the way. Incredibly noisy, not recommended.

(defmulti verbose {:arglists '([context m])} #'reporter-dispatch)
(defmethod verbose :default [_ _])

(def type->name
  {
   :begin-test-run "test run"
   :begin-ns-suite "namespace suite"
   :begin-test-var "test var"
   :begin-test-suite "suite"
   :begin-test-seq "suite"
   :begin-test-case "test case"
   :end-test-run "test run"
   :end-ns-suite "namespace suite"
   :end-test-var "test var"
   :end-test-suite "suite"
   :end-test-seq "suite"
   :end-test-case "test case"
   })

(defn print-entering [s]
  (println "Running" (str (type->name (:type s)) ":")
           (str (s/identifier s)
                (when (and (:file s) (:line s))
                  (str " (" (:file s) ":" (:line s) ")")))))

(defn print-leaving [s]
  (println "Done with" (str (type->name (:type s)) ":")
           (str (s/identifier s)
                (when (and (:file s) (:line s))
                  (str " (" (:file s) ":" (:line s) ")")))))

(defmethod verbose :begin-test-run [_ _] (println "Starting test run"))
(defmethod verbose :begin-ns-suite [_context s] (print-entering s))
(defmethod verbose :begin-test-var [_context s] (print-entering s))
(defmethod verbose :begin-test-suite [_context s] (print-entering s))
(defmethod verbose :begin-test-seq [_context s] (print-entering s))

(defmethod verbose :end-test-run [_ _] (println "Ending test run"))
(defmethod verbose :end-ns-suite [_context s] (print-leaving s))
(defmethod verbose :end-test-var [_context s] (print-leaving s))
(defmethod verbose :end-test-suite [_context s] (print-leaving s))
(defmethod verbose :end-test-seq [_context s] (print-leaving s))

(defn print-entering-tc [tc]
  (println "Running" (str (type->name (:type tc)) ":")
           (str (tc/identifier tc) " (" (:file tc) ":" (:line tc) ")")))

(defn print-leaving-tc [tc]
  (println "Done with" (str (type->name (:type tc)) ":")
           (str (tc/identifier tc) " (" (:file tc) ":" (:line tc) ")")))

(defmethod verbose :begin-test-case [_context tc] (print-entering-tc tc))
(defmethod verbose :end-test-case [_context tc] (print-leaving-tc tc))

(defmethod verbose :pass [_context result] (prn result))
(defmethod verbose :fail [_context result] (prn result))
(defmethod verbose :error [_context result] (prn result))

;;; PROFILE
;;; Print the top 5 namespaces and test vars by duration.
;;; Code adapted from kaocha
;;;
;;; Example:
;;;
;;; blah blah blah

(defmulti profile {:arglists '([context m])} #'reporter-dispatch)
(defmethod profile :default [_ _])
(defmethod profile :end-test-run [_context {:keys [results]}]
  (let [types (-> (group-by (comp type :source) (result-seq results))
                  (select-keys [:lazytest/ns-suite :lazytest/test-var])
                  (update-vals #(filterv :lazytest.runner/duration %)))
        total-duration (->> (mapcat identity (vals types))
                            (map :lazytest.runner/duration)
                            (reduce + 0))
        slowest-ns-suites (take 5 (sort-by :lazytest.runner/duration > (:lazytest/ns-suite types)))
        ns-suite-duration (reduce + 0 (map :lazytest.runner/duration slowest-ns-suites))
        slowest-vars (take 5 (sort-by :lazytest.runner/duration > (:lazytest/test-var types)))
        var-duration (reduce + 0 (map :lazytest.runner/duration slowest-vars))
        ]
    (println (format "Top %s slowest test namespaces (%.5f seconds, %.1f%% of total time)"
                     (count slowest-ns-suites)
                     (double (/ ns-suite-duration 1e9))
                     (double (* (/ ns-suite-duration total-duration) 100))))
    (println (->> (for [suite slowest-ns-suites]
                    (format "  %s %.5f seconds"
                            (s/identifier suite)
                            (double (/ (:lazytest.runner/duration suite) 1e9))))
                  (str/join \newline)))
    (newline)
    (println (format "Top %s slowest test vars (%.5f seconds, %.1f%% of total time)"
                     (count slowest-vars)
                     (double (/ var-duration 1e9))
                     (double (* (/ var-duration total-duration) 100))))
    (println (->> (for [suite slowest-vars]
                    (format "  %s %.5f seconds"
                            (s/identifier suite)
                            (double (/ (:lazytest.runner/duration suite) 1e9))))
                  (str/join \newline)))
    (flush)))
