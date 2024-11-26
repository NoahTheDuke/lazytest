(ns lazytest.find-test
  (:require
   [clojure.string :as str]
   [lazytest.core :refer [after before defdescribe describe expect it]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.main :as main]
   [lazytest.runner :as lr]
   [lazytest.test-utils :refer [with-out-str-data-map]]))

(defdescribe find-var-test-value-test
  (let [output (volatile! nil)]
    (describe "outer"
      {:context [(before
                  (load-file "corpus/find_tests/examples.clj")
                  (require 'find-tests.examples)
                  (vreset! output (with-out-str-data-map
                                    (main/run ["--dir" "corpus/find_tests"
                                               "--output" "nested*"]))))
                 (after (vreset! output nil))]}
      (it "runs all suites"
        (expect
          (match?
           {:total 4
            :pass 0
            :fail 4
            :exit 1
            :results
            {::lr/source-type :lazytest/run
             :children
             [{::lr/source-type :lazytest/ns
               :doc 'find-tests.examples
               :children
               [{::lr/source-type :lazytest/var
                 :doc (resolve 'find-tests.examples/test-fn)
                 :children
                 [{:type :fail
                   :doc "`:lazytest/test` metadata"
                   :thrown (ex-info "Expectation failed"
                                    {:type 'lazytest.ExpectationFailed
                                     :expected '(zero? (test-fn 1))})}]}
                {::lr/source-type :lazytest/var
                 :doc (resolve 'find-tests.examples/test-test-case)
                 :children
                 [{:type :fail
                   :doc "test case example"
                   :thrown (ex-info "Expectation failed"
                                    {:type 'lazytest.ExpectationFailed
                                     :expected '(= 1 (test-test-case 1))})}]}
                {::lr/source-type :lazytest/var
                 :doc (resolve 'find-tests.examples/test-describe)
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
                                       :expected '(zero? (test-describe 1))})}]}]}]}]}}
           (:result @output))))
      (it "prints as expected"
        (expect (=
                 (str/join
                  "\n"
                  ["  find-tests.examples"
                   "    #'find-tests.examples/test-fn"
                   "      × `:lazytest/test` metadata FAIL"
                   "    #'find-tests.examples/test-test-case"
                   "      × test case example FAIL"
                   "    #'find-tests.examples/test-describe"
                   "      top level"
                   "        × test-describe example FAIL"
                   "        × test-describe example two FAIL"])
                 (str/trimr (:string @output))))))))
