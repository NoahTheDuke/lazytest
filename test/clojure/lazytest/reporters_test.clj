(ns lazytest.reporters-test
  (:require
   [clojure.stacktrace :as stack]
   [clojure.string :as str]
   [lazytest.clojure-ext.core :refer [re-compile]]
   [lazytest.config :refer [->config]]
   [lazytest.core :refer [defdescribe describe expect expect-it it around]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.reporters :as sut]
   [lazytest.runner :as runner]
   [lazytest.suite :as s :refer [suite]]
   [lazytest.test-case :as tc]
   [lazytest.test-utils :refer [with-out-str-no-color]]))

(set! *warn-on-reflection* true)

(defdescribe focused-test
  (expect-it "prints correctly"
    (= "=== FOCUSED TESTS ONLY ===\n\n"
       (-> (sut/focused nil {:type :begin-test-run
                             :metadata {:focus true}})
           (with-out-str-no-color))))
  (expect-it "prints nothing if not focused"
    (nil? (->> (sut/focused nil {:type :begin-test-run
                                 :metadata {:focus false}})
               (with-out-str-no-color)))))

(defn make-suite [& results]
  ^{:type ::s/suite-result}
  {:type ::s/suite-result
   :lazytest.runner/duration 123456789.0
   :doc "example suite"
   :children results})

(defn ->passing [& {:as extra}]
  (-> (tc/test-case {:doc "example test-case"
                     :body (fn [] true)})
      (runner/run-tree (->config {:output [(constantly nil)]}))
      (merge extra)))

(defn ->failing [& {:as extra}]
  (-> (tc/test-case {:doc "example test-case"
                     :body (fn [] (expect (= 1 2) (if (contains? extra :message)
                                                    (:message extra)
                                                    "failing")))})
      (runner/run-tree (->config {:output [(constantly nil)]}))
      (assoc :file "example.clj")
      (assoc :line 1)
      (merge (dissoc extra :message))))

(defn ->erroring [& _]
  (let [thrown (ex-info "deliberate error"
                        {:ex :info
                         :file "example.clj"
                         :line 1})]
    (-> (tc/test-case {:doc "example test-case"
                       :body (fn [] (throw thrown))})
        (runner/run-tree (->config {:output [(constantly nil)]})))))

(defn stub-stack-trace
  [_ex]
  (print "<stack-trace>"))

(defdescribe summary-test
  (it "no tests"
    (expect
      (= "Ran 0 test cases in 0.12346 seconds.\n0 failures.\n"
         (-> (sut/summary nil {:type :end-test-run
                               :results (make-suite)})
             (with-out-str-no-color)))))
  (it "passing"
    (expect
      (= "Ran 1 test cases in 0.12346 seconds.\n0 failures.\n"
         (-> (sut/summary nil {:type :end-test-run
                               :results (make-suite (->passing))})
             (with-out-str-no-color)))))
  (it "failures"
    (expect
      (= "Ran 1 test cases in 0.12346 seconds.\n1 failure.\n"
         (-> (sut/summary nil {:type :end-test-run
                               :results (make-suite (->failing))})
             (with-out-str-no-color)))))
  (it "errors"
    (expect
      (= "Ran 1 test cases in 0.12346 seconds.\n1 failure.\n"
         (-> (sut/summary nil {:type :end-test-run
                               :results (make-suite (->erroring))})
             (with-out-str-no-color)))))
  (it "combinations"
    (expect
      (= "Ran 3 test cases in 0.12346 seconds.\n2 failures.\n"
         (-> (sut/summary nil {:type :end-test-run
                               :results (make-suite
                                          (->passing)
                                          (->failing)
                                          (->erroring))})
             (with-out-str-no-color)))))
  (expect-it "handles defaults"
    (nil? (-> (sut/summary nil {:type :anything-else})
              (with-out-str-no-color)))))

(defdescribe results-test
  {:context [(around [f]
              (with-redefs [stack/print-trace-element stub-stack-trace]
                (f)))]}
  (it "no tests"
    (expect
      (nil?
         (-> (sut/results nil {:type :end-test-run
                               :results (make-suite)})
             (with-out-str-no-color)))))
  (it "passing"
    (expect
      (nil?
         (-> (sut/results nil {:type :end-test-run
                               :results (make-suite (->passing))})
             (with-out-str-no-color)))))
  (describe "failures"
    (it "prints the expectation's message"
      (expect
        (= (str/join \newline
                     ["example suite"
                      "  example test-case:"
                      ""
                      "failing"
                      "Expected: (= 1 2)"
                      "Actual: false"
                      "Evaluated arguments:"
                      " * 1"
                      " * 2"
                      "Only in first argument:"
                      "1"
                      "Only in second argument:"
                      "2"
                      ""
                      "in example.clj:1"
                      ""
                      ""])
           (-> (sut/results nil {:type :end-test-run
                                 :results (make-suite (->failing))})
               (with-out-str-no-color)))))
    (it "defaults if given no message"
      (expect
        (= (str/join \newline
                     ["example suite"
                      "  example test-case:"
                      ""
                      "Expectation failed"
                      "Expected: (= 1 2)"
                      "Actual: false"
                      "Evaluated arguments:"
                      " * 1"
                      " * 2"
                      "Only in first argument:"
                      "1"
                      "Only in second argument:"
                      "2"
                      ""
                      "in example.clj:1"
                      ""
                      ""])
           (-> (sut/results nil {:type :end-test-run
                                 :results (make-suite (->failing :message nil))})
               (with-out-str-no-color))))))
  (describe "errors"
    (it "prints the given message"
      (expect
        (= (str/join \newline ["example suite"
                               "  example test-case:"
                               ""
                               "clojure.lang.ExceptionInfo: deliberate error"
                               "Expected: nil"
                               "Actual: nil"
                               ""
                               "<stack-trace>"
                               ""
                               "in example.clj:1"
                               ""
                               ""])
           (-> (sut/results nil {:type :end-test-run
                                 :results (make-suite (->erroring))})
               (with-out-str-no-color)))))
    (it "defaults if given no message"
      (expect
        (= (str/join \newline ["example suite"
                               "  example test-case:"
                               ""
                               "clojure.lang.ExceptionInfo: deliberate error"
                               "Expected: nil"
                               "Actual: nil"
                               ""
                               "<stack-trace>"
                               ""
                               "in example.clj:1"
                               ""
                               ""])
           (-> (sut/results nil {:type :end-test-run
                                 :results (make-suite (->erroring :message nil))})
               (with-out-str-no-color))))))
  (it "combinations"
    (expect
      (= (str/join \newline
                   ["example suite"
                    "  example test-case:"
                    ""
                    "failing"
                    "Expected: (= 1 2)"
                    "Actual: false"
                    "Evaluated arguments:"
                    " * 1"
                    " * 2"
                    "Only in first argument:"
                    "1"
                    "Only in second argument:"
                    "2"
                    ""
                    "in example.clj:1"
                    ""
                    "example suite"
                    "  example test-case:"
                    ""
                    "clojure.lang.ExceptionInfo: deliberate error"
                    "Expected: nil"
                    "Actual: nil"
                    ""
                    "<stack-trace>"
                    ""
                    "in example.clj:1"
                    ""
                    ""])
         (-> (sut/results nil {:type :end-test-run
                               :results (make-suite
                                          (->passing)
                                          (->failing)
                                          (->erroring))})
             (with-out-str-no-color)))))
  (expect-it "handles defaults"
    (nil? (-> (sut/results nil {:type :anything-else})
              (with-out-str-no-color)))))

(defdescribe dots*-test
  (describe "test cases"
    (expect-it "passing"
      (= "." (-> (sut/dots* nil (->passing))
                 (with-out-str-no-color))))
    (expect-it "failing"
      (= "F" (-> (sut/dots* nil (->failing))
                 (with-out-str-no-color))))
    (expect-it "erroring"
      (= "F" (-> (sut/dots* nil (->erroring))
                 (with-out-str-no-color)))))
  (describe "ns-test-suite"
    (it "begin"
      (expect (= "(" (-> (sut/dots* nil {:type :begin-test-ns})
                         (with-out-str-no-color)))))
    (it "end"
      (expect (= ")" (-> (sut/dots* nil {:type :end-test-ns})
                         (with-out-str-no-color))))))
  (describe "end test run"
    (it "adds a newline"
      (expect (= "\n" (-> (sut/dots* nil {:type :end-test-run})
                          (with-out-str-no-color))))))
  (expect-it "handles defaults"
    (nil? (-> (sut/dots* nil {:type :anything-else})
              (with-out-str-no-color)))))

(defn make-big-suite [opts]
  ^{:type ::s/suite-result}
  {:type (:type opts)
   :doc (:doc opts)
   :var (:var opts)
   :children
   (or (:children opts)
       [(-> (make-suite (:suite1 opts))
            (assoc :doc "suite 1"))
        (-> (make-suite (:suite2 opts))
            (assoc :doc "suite 1"))])})

(defdescribe nested-test
  (describe "prints various types"
    (let [ctx (->config nil)]
      (it "prints suites"
        (expect
          (= "  example doc\n"
             (-> (sut/nested* ctx (make-big-suite {:type :begin-test-run
                                                   :doc "example doc"}))
                 (with-out-str-no-color)))))
      (it "prints namespaces"
        (expect
          (= "  example.namespace\n"
             (-> (sut/nested* ctx (make-big-suite {:type :begin-test-run
                                                   :doc "example.namespace"}))
                 (with-out-str-no-color)))))
      (it "prints vars"
        (expect
          (= "  #'clojure.core/identity\n"
             (-> (sut/nested* ctx (make-big-suite {:type :begin-test-run
                                                   :var #'identity}))
                 (with-out-str-no-color)))))))
  (describe "all of the begin-* seq types"
    (let [ctx (->config nil)]
      (map (fn [t]
             (it (str "prints " t)
               (expect
                 (= "  example doc\n"
                    (-> (sut/nested* ctx (make-big-suite {:type t
                                                          :doc "example doc"}))
                        (with-out-str-no-color))))))
           [:begin-test-run :begin-test-ns :begin-test-var :begin-test-suite])))
  (describe "test case results"
    (let [ctx (->config nil)]
      (it "pass"
        (expect
          (= "  √ example test-case\n"
             (-> (sut/nested* ctx (->passing))
                 (with-out-str-no-color)))))
      (it "fail"
        (expect
          (= "  × example test-case FAIL\n"
             (-> (sut/nested* ctx (->failing))
                 (with-out-str-no-color)))))
      (it "error"
        (expect
          (= "  × example test-case FAIL\n"
             (-> (sut/nested* ctx (->erroring))
                 (with-out-str-no-color)))))))
  (describe "indentation"
    (it "defaults to 2 spaces"
      (let [out (-> (sut/nested* (->config nil) (->passing))
                    (with-out-str-no-color))]
        (expect
          (= 2 (- (count out) (count (str/triml out)))))))
    (it "increases by two for every depth"
      (let [depth 3
            out (-> (sut/nested* (-> (->config nil)
                                     (assoc :lazytest.runner/depth depth))
                                 (->passing))
                    (with-out-str-no-color))]
        (expect
          (= (* 2 depth) (- (count out) (count (str/triml out)))))))))

(defdescribe profile-test
  (it "prints the right times"
    (let [test-suite (-> (describe "suite"
                           (it "test case" (expect (= 2 1))))
                         (assoc :type :lazytest/var :var #'profile-test))
          results (runner/run-test-suite
                    (suite {:type :lazytest/ns
                            :nses [*ns*]
                            :children [test-suite]})
                    (->config {:output (constantly nil)}))
          suite-results (assoc test-suite :type :end-test-run :results results)]
      (expect (match? (re-compile "Top 1 slowest test namespaces.*Top 1 slowest test vars" :dotall)
                      (-> (sut/profile nil suite-results)
                          (with-out-str-no-color)))))))

(defdescribe defdescribe-no-doc nil (it "works" nil))
(defdescribe defdescribe-with-doc "cool docs" (it "works" nil))

(defdescribe defdescribe-metadata-test
  (it "uses the var if given no doc string"
    (expect (= "  defdescribe-no-doc\n    √ works\n\n"
               (-> (runner/run-test-var #'defdescribe-no-doc
                                        (->config {:output sut/nested*}))
                   (with-out-str-no-color)))))
  (it "uses the doc string when available"
    (expect (= "  cool docs\n    √ works\n\n"
               (-> (runner/run-test-var #'defdescribe-with-doc
                                        (->config {:output sut/nested*}))
                   (with-out-str-no-color))))))
