(ns lazytest.reporters
  (:require
   [clojure.data :refer [diff]]
   [clojure.pprint :as pp]
   [clojure.stacktrace :as stack]
   [clojure.string :as str]
   [lazytest.color :refer [colorize]]
   [lazytest.results :refer [result-seq summarize]]
   [lazytest.suite :as s :refer [suite-result?]]
   [lazytest.test-case :as tc])
  (:import
   lazytest.ExpectationFailed))

(defn report [config m]
  (when-let [reporter (:reporter config)]
    (reporter config m)))

(defn reporter-dispatch [_config m] (:type m))

(defn- indent [n]
  (print (apply str (repeat n "  "))))

;; FOCUSED
;; Prints a message when tests are focused.
;;
;; Example:
;;
;; === FOCUSED TESTS ONLY ===

(defmulti focused {:arglists '([config m])} #'reporter-dispatch)
(defmethod focused :default [_ _])
(defmethod focused :begin-test-run [_ m]
  (when (-> m :metadata :focus)
    (println "=== FOCUSED TESTS ONLY ===")
    (newline))
  (flush))

;; SUMMARY
;; Prints the number of test cases and failures.
;;
;; Example:
;;
;; Ran 5 test cases in 0.04501 seconds.
;; 0 failures.

(defmulti summary {:arglists '([config m])} #'reporter-dispatch)
(defmethod summary :default [_ _])
(defmethod summary :end-test-run [_ m]
  (let [{:keys [total fail]} (summarize (:results m))
        duration (double (/ (-> m :results (:lazytest.runner/duration 0.0)) 1e9))
        count-msg (format "Ran %s test cases in %.5f seconds." total duration)]
    (println (if (zero? total)
               (colorize count-msg :yellow)
               count-msg))
    (println (colorize (str fail " failure" (when (not= 1 fail) "s") ".")
                       (if (zero? fail) :green :red)))
    (newline)
    (flush)))

;; RESULTS
;; Print the failed assertions, their arguments, and associated information.
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

(defmulti ^:private results-builder {:arglists '([config m])} #'reporter-dispatch)

(defmethod results-builder ::s/suite-result
  [config {:keys [children] :as results}]
  (doseq [child children
          :let [docs (conj (:docs results []) (s/identifier results))
                child (assoc child :docs docs)]]
    (results-builder config child)))

(defn- print-docs [docs]
  (loop [[doc & docs] (seq (->> docs
                                (filter identity)
                                (remove #(when (string? %) (str/blank? %)))))
         idx 0]
    (when doc
      (indent idx)
      (if docs
        (println doc)
        (println (str doc ":")))
      (recur docs (inc idx)))))

(defn- print-evaluated-arguments [result]
  (println (colorize "Evaluated arguments:" :cyan))
  (doseq [arg (rest (:evaluated result))]
    (print " * ")
    (if (instance? Throwable arg)
      (pp/pprint (type arg))
      (pp/pprint arg))))

(defn- print-equality-failed [[_ a b]]
  (let [[a b same] (diff a b)]
    (println (colorize "Only in first argument:" :cyan))
    (pp/pprint a)
    (println (colorize "Only in second argument:" :cyan))
    (pp/pprint b)
    (when (some? same)
      (println (colorize "The same in both:" :cyan))
      (pp/pprint same))))

(defmethod results-builder :pass [_ _])

(defn print-stack-trace
  "Adapted from clojure.stacktrace/print-stack-trace"
  [^Throwable t n]
  (let [st (.getStackTrace t)]
    (when-let [e (first st)]
      (stack/print-trace-element e)
      (newline)
      (doseq [e (if (nil? n)
                  (rest st)
                  (take (dec n) (rest st)))]
        (print "    ")
        (stack/print-trace-element e)
        (newline))
      (newline))))

(defmethod results-builder :fail [_config result]
  (print-docs (conj (:docs result) (tc/identifier result)))
  (let [message (str (when-not (instance? ExpectationFailed (:thrown result))
                       (str (.getName (class (:thrown result))) ": "))
                     (:message result))]
    (newline)
    (println (colorize message :red))
    (println (colorize "Expected:" :cyan)
             (pr-str (:expected result)))
    (println (colorize "Actual:" :cyan)
             (pr-str (:actual result)))
    (when (:evaluated result)
      (print-evaluated-arguments result)
      (when (and (= = (first (:evaluated result)))
                 (= 3 (count (:evaluated result))))
        (print-equality-failed (:evaluated result))))
    (newline)
    (print-stack-trace (:thrown result) 1)
    (println (colorize (format "in %s:%s\n" (:file result) (:line result)) :light)))
  (flush))

(defmulti results {:arglists '([config m])} #'reporter-dispatch)
(defmethod results :default [_ _])
(defmethod results :end-test-run [config m]
  (results-builder config (:results m)))

;; DOTS
;; Passing test cases are printed as `.`, and failures as `F`.
;; Test cases in namespaces are wrapped in parentheses.
;;
;; Example:
;;
;; (....)(F)(.F..)

(defmulti dots* {:arglists '([config m])} #'reporter-dispatch)
(defmethod dots* :default [_ _])
(defmethod dots* :pass [_ _] (print (colorize "." :green)))
(defmethod dots* :fail [_ _] (print (colorize "F" :red)))
(defmethod dots* :begin-test-ns [_ _] (print (colorize "(" :yellow)))
(defmethod dots* :end-test-ns [_ _] (print (colorize ")" :yellow)))
(defmethod dots* :end-test-run [_ _] (newline))

(def dots
  [focused dots* results summary])

;; NESTED
;; Print each suite and test case on a new line, and indent each suite.
;;
;; Example:
;;
;;  lazytest.core-test
;;    it-test
;;      √ will early exit
;;      √ arbitrary code
;;    with-redefs-test
;;      redefs inside 'it' blocks
;;        × should be rebound FAIL
;;      redefs outside 'it' blocks
;;        √ should not be rebound

(defmulti nested* {:arglists '([config m])} #'reporter-dispatch)
(defmethod nested* :default [_ _])

(defn print-test-seq
  [config s]
  (let [id (s/identifier s)
        depth (:lazytest.runner/depth config)]
    (when id
      (indent depth)
      (println id))))

(defmethod nested* :begin-test-run [config s] (print-test-seq config s))
(defmethod nested* :begin-test-ns [config s] (print-test-seq config s))
(defmethod nested* :begin-test-var [config s] (print-test-seq config s))
(defmethod nested* :begin-test-suite [config s] (print-test-seq config s))
(defmethod nested* :end-test-run [_ _] (newline) (flush))

(defn print-test-result
  [config result]
  (let [id (tc/identifier result)]
    (indent (:lazytest.runner/depth config))
    (let [result-type (:type result)
          msg (str id (when (not= :pass result-type)
                        (str " " (str/upper-case (name result-type)))))]
      (case result-type
        :pass (println (colorize "√" :green) (colorize msg :light))
        :pending (println (colorize "-" :cyan) (colorize msg :cyan))
        :fail (println (colorize "×" :red) (colorize msg :red))
        #_:else nil))))

(defmethod nested* :pass [config result] (print-test-result config result))
(defmethod nested* :fail [config result] (print-test-result config result))

(def nested
  [focused nested* results summary])

;; CLOJURE-TEST
;; Adapts clojure.test's default reporter to Lazytests' system.
;; It treats suite and test-case :docs as testing strings.
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

(defmulti clojure-test {:arglists '([config m])} #'reporter-dispatch)
(defmethod clojure-test :default [_ _])

(defn- clojure-test-case-str [config result]
  (format "(%s) (%s:%s)"
          (->> (:lazytest.runner/suite-history config)
               (keep :var)
               (map #(:name (meta %)))
               (str/join " "))
          (:file result)
          (:line result)))

(defn clojure-test-fail [config result]
  (println "\nFAIL in" (clojure-test-case-str config result))
  (when-let [strings (->> (conj (:lazytest.runner/suite-history config) result)
                          (keep :doc)
                          (drop 1)
                          (seq))]
    (println (str/join " " strings)))
  (when-let [message (:message result)]
    (println message))
  (println "expected:" (pr-str (:expected result)))
  (println "  actual:" (pr-str (:actual result))))

(defn clojure-test-error [config result]
  (println "\nERROR in" (clojure-test-case-str config result))
  (when-let [strings (->> (conj (:lazytest.runner/suite-history config) result)
                          (keep :doc)
                          (drop 1)
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

(defmethod clojure-test :fail [config result]
  (if (instance? ExpectationFailed (:thrown result))
    (clojure-test-fail config result)
    (clojure-test-error config result)))

(defmethod clojure-test :begin-test-ns [_ result]
  (println "\nTesting" (ns-name (:doc result))))

(defmethod clojure-test :end-test-run [_ m]
  (let [results (:results m)
        test-vars (count (filter #(= :lazytest/var (:type (:source %)))
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

;; DEBUG
;; Prints loudly about every step of the way. Incredibly noisy, not recommended.

(defmulti debug {:arglists '([config m])} #'reporter-dispatch)
(defmethod debug :default [_ _])

(def type->name
  {
   :begin-test-run "test run"
   :begin-test-ns "namespace suite"
   :begin-test-var "test var"
   :begin-test-suite "suite"
   :begin-test-seq "suite"
   :begin-test-case "test case"
   :end-test-run "test run"
   :end-test-ns "namespace suite"
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

(defmethod debug :begin-test-run [_ _] (println "Starting test run"))
(defmethod debug :begin-test-ns [_config s] (print-entering s))
(defmethod debug :begin-test-var [_config s] (print-entering s))
(defmethod debug :begin-test-suite [_config s] (print-entering s))
(defmethod debug :begin-test-seq [_config s] (print-entering s))

(defmethod debug :end-test-run [_ _] (println "Ending test run"))
(defmethod debug :end-test-ns [_config s] (print-leaving s))
(defmethod debug :end-test-var [_config s] (print-leaving s))
(defmethod debug :end-test-suite [_config s] (print-leaving s))
(defmethod debug :end-test-seq [_config s] (print-leaving s))

(defn print-entering-tc [tc]
  (println "Running" (str (type->name (:type tc)) ":")
           (str (tc/identifier tc) " (" (:file tc) ":" (:line tc) ")")))

(defn print-leaving-tc [tc]
  (println "Done with" (str (type->name (:type tc)) ":")
           (str (tc/identifier tc) " (" (:file tc) ":" (:line tc) ")")))

(defmethod debug :begin-test-case [_config tc] (print-entering-tc tc))
(defmethod debug :end-test-case [_config tc] (print-leaving-tc tc))

(defmethod debug :pass [_config result] (prn result))
(defmethod debug :fail [_config result] (prn result))

;; PROFILE
;; Print the top 5 namespaces and test vars by duration.
;; Code adapted from kaocha
;;
;; Example:
;;
;; blah blah blah

(defmulti profile {:arglists '([config m])} #'reporter-dispatch)
(defmethod profile :default [_ _])
(defmethod profile :end-test-run [_config {:keys [results]}]
  (let [types (-> (group-by (comp :type :source) (result-seq results))
                  (select-keys [:lazytest/ns :lazytest/var])
                  (update-vals #(filterv :lazytest.runner/duration %)))
        total-duration (->> (mapcat identity (vals types))
                            (map :lazytest.runner/duration)
                            (reduce + 0))
        slowest-ns-suites (take 5 (sort-by :lazytest.runner/duration > (:lazytest/ns types)))
        ns-suite-duration (reduce + 0 (map :lazytest.runner/duration slowest-ns-suites))
        slowest-vars (take 5 (sort-by :lazytest.runner/duration > (:lazytest/var types)))
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
    (println (->> (for [suite slowest-vars
                        :let [ns (some-> suite :source :var symbol namespace)
                              id (str ns (when ns "/") (s/identifier suite))]]
                    (format "  %s %.5f seconds"
                            id
                            (double (/ (:lazytest.runner/duration suite) 1e9))))
                  (str/join \newline)))
    (flush)))

;; QUIET
;; Print nothing.

(defmulti quiet {:arglists '([config m])} #'reporter-dispatch)
(defmethod quiet :default [_ _])
