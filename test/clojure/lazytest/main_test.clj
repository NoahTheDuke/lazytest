(ns lazytest.main-test
  (:require
   [lazytest.core :refer [defdescribe expect it]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.main :as sut]
   [lazytest.runner :as-alias lr]))

(defdescribe filter-ns-test
  (it "selects the right namespace"
    (expect
      (match?
       {:total 3
        :pass 3
        :fail 0
        :exit 0
        :results {::lr/source-type :lazytest/run
                  :children [{::lr/source-type :lazytest/ns-suite
                              :doc 'filter-tests.a
                              :children [{::lr/source-type :lazytest/test-var
                                          :doc "a-1-test"}
                                         {::lr/source-type :lazytest/test-var
                                          :doc "a-2-test"}
                                         {::lr/source-type :lazytest/test-var
                                          :doc "a-3-test"}]}]}}
       (sut/run ["--output" "quiet"
                 "--dir" "corpus"
                 "--namespace" "filter-tests.a"])))
    (expect
      (match?
       {:total 3
        :pass 3
        :fail 0
        :exit 0
        :results {::lr/source-type :lazytest/run
                  :children [{::lr/source-type :lazytest/ns-suite
                              :doc 'filter-tests.b
                              :children [{::lr/source-type :lazytest/test-var
                                          :doc "b-1-test"}
                                         {::lr/source-type :lazytest/test-var
                                          :doc "b-2-test"}
                                         {::lr/source-type :lazytest/test-var
                                          :doc "b-3-test"}]}]}}
       (sut/run ["--output" "quiet"
                 "--dir" "corpus"
                 "--namespace" "filter-tests.b"]))))
  (it "selects the right var"
    (expect
      (match?
       {:total 1
        :pass 1
        :fail 0
        :exit 0
        :results {::lr/source-type :lazytest/run
                  :children [{::lr/source-type :lazytest/ns-suite
                              :doc 'filter-tests.a
                              :children [{::lr/source-type :lazytest/test-var
                                          :doc "a-1-test"}]}]}}
       (sut/run ["--output" "quiet"
                 "--dir" "corpus"
                 "--var" "filter-tests.a/a-1-test"]))))
    (it "selects multiple vars"
      (expect
        (match?
         {:total 2
          :pass 2
          :fail 0
          :exit 0
          :results {::lr/source-type :lazytest/run
                    :doc nil?
                    :children [{::lr/source-type :lazytest/ns-suite
                                :doc 'filter-tests.a
                                :children [{::lr/source-type :lazytest/test-var
                                            :doc "a-1-test"}]}
                               {::lr/source-type :lazytest/ns-suite
                                :doc 'filter-tests.b
                                :children [{::lr/source-type :lazytest/test-var
                                            :doc "b-2-test"}]}]}}
         (sut/run ["--output" "quiet"
                   "--dir" "corpus"
                   "--var" "filter-tests.a/a-1-test"
                   "--var" "filter-tests.b/b-2-test"]))))
  (it "handles mismatched var and namespace filters"
    (expect
      (match?
       {:total 4
        :pass 4
        :fail 0
        :exit 0
        :results {::lr/source-type :lazytest/run
                  :doc nil?
                  :children [{::lr/source-type :lazytest/ns-suite
                              :doc 'filter-tests.a
                              :children [{::lr/source-type :lazytest/test-var
                                          :doc "a-1-test"}
                                         {::lr/source-type :lazytest/test-var
                                          :doc "a-2-test"}
                                         {::lr/source-type :lazytest/test-var
                                          :doc "a-3-test"}]}
                             {::lr/source-type :lazytest/ns-suite
                              :doc 'filter-tests.b
                              :children [{::lr/source-type :lazytest/test-var
                                          :doc "b-2-test"}]}]}}
       (sut/run ["--output" "quiet"
                 "--dir" "corpus"
                 "--namespace" "filter-tests.a"
                 "--var" "filter-tests.b/b-2-test"])))))
