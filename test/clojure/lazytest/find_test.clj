(ns lazytest.find-test
  (:require
   [find-tests.examples]
   [lazytest.core :refer [defdescribe expect given it]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.main :as main]
   [lazytest.runner :as lr]
   [lazytest.test-utils :refer [with-out-str-data-map]]
   [clojure.string :as str]))

(defdescribe find-var-test-value-test
  (given [output (with-out-str-data-map (main/run ["--dir" "corpus/find_tests"
                                                   "--output" "nested*"]))]
    (it "runs all suites"
      (expect
        (match?
         {:total 6
          :pass 0
          :fail 6
          :exit 1
          :results
          {::lr/source-type :lazytest/run
           :children
           [{::lr/source-type :lazytest/ns-suite
             :doc 'find-tests.examples
             :children
             [{::lr/source-type :lazytest/test-var
               :var #'find-tests.examples/test-fn
               :doc #'find-tests.examples/test-fn
               :children
               [{:type :fail
                 :doc "`:test` metadata"
                 :thrown (ex-info "Expectation failed"
                                  {:type 'lazytest.ExpectationFailed
                                   :expected '(= 0 (test-fn 1))})}]}
              {::lr/source-type :lazytest/test-var
               :var #'find-tests.examples/test-test-case
               :doc #'find-tests.examples/test-test-case
               :children
               [{:type :fail
                 :doc "test case example"
                 :thrown (ex-info "Expectation failed"
                                  {:type 'lazytest.ExpectationFailed
                                   :expected '(= 1 (test-test-case 1))})}]}
              {::lr/source-type :lazytest/test-var
               :var #'find-tests.examples/test-suite
               :doc #'find-tests.examples/test-suite
               :children
               [{::lr/source-type :lazytest/suite
                 :doc nil
                 :children
                 [{:type :fail
                   :doc "test-seq example"
                   :thrown (ex-info "Expectation failed"
                                    {:type 'lazytest.ExpectationFailed
                                     :expected '(= 1 (test-suite 1))})}
                  {:type :fail
                   :doc "test-seq example two"
                   :thrown (ex-info "Expectation failed"
                                    {:type 'lazytest.ExpectationFailed
                                     :expected '(= 0 (test-suite 1))})}]}]}
              {::lr/source-type :lazytest/test-var
               :var #'find-tests.examples/test-describe
               :doc #'find-tests.examples/test-describe
               :children
               [{::lr/source-type :lazytest/suite
                 :doc "top level"
                 :children
                 [{:type :fail
                   :doc "test-describe example"
                   :thrown (ex-info "Expectation failed"
                                    {:type 'lazytest.ExpectationFailed
                                     :expected '(= 1 (test-describe 1))})}
                  {:type :fail
                   :doc "test-describe example two"
                   :thrown (ex-info "Expectation failed"
                                    {:type 'lazytest.ExpectationFailed
                                     :expected '(= 0 (test-describe 1))})}]}]}]}]}}
         (:result output))))
    (it "prints as expected"
      (expect (=
               (str/join
                "\n"
                ["  find-tests.examples"
                 "    #'find-tests.examples/test-fn"
                 "      × `:test` metadata FAIL"
                 "    #'find-tests.examples/test-test-case"
                 "      × test case example FAIL"
                 "    #'find-tests.examples/test-suite"
                 "      × test-seq example FAIL"
                 "      × test-seq example two FAIL"
                 "    #'find-tests.examples/test-describe"
                 "      top level"
                 "        × test-describe example FAIL"
                 "        × test-describe example two FAIL"])
                 (str/trimr (:string output)))))))
