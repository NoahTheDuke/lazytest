(ns lazytest.main-test
  (:require
   [lazytest.core :refer [defdescribe describe expect it]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.main :as sut]
   [lazytest.runner :as-alias lr]))

(defdescribe example-test
  (describe "wtf"
    (it "works?" (expect (= 1 1)))))

(defdescribe filter-ns-test
  (describe "--namespace"
    (it "selects the right namespace"
      (expect
        (match?
         {:total 3
          :pass 3
          :fail 0
          :exit 0
          :results {::lr/source-type :lazytest/run
                    :children [{::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.a
                                :children [{::lr/source-type :lazytest/var
                                            :doc "a-1-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "a-2-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "a-3-test"}]}]}}
         (sut/run ["--output" "quiet"
                   "--dir" "corpus"
                   "--namespace" "cli-filter-tests.a"])))
      (expect
        (match?
         {:total 3
          :pass 3
          :fail 0
          :exit 0
          :results {::lr/source-type :lazytest/run
                    :children [{::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.b
                                :children [{::lr/source-type :lazytest/var
                                            :doc "b-1-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "b-2-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "b-3-test"}]}]}}
         (sut/run ["--output" "quiet"
                   "--dir" "corpus"
                   "--namespace" "cli-filter-tests.b"])))))
  (describe "--var"
    (it "selects the right var"
      (expect
        (match?
         {:total 1
          :pass 1
          :fail 0
          :exit 0
          :results {::lr/source-type :lazytest/run
                    :children [{::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.a
                                :children [{::lr/source-type :lazytest/var
                                            :doc "a-1-test"}]}]}}
         (sut/run ["--output" "quiet"
                   "--dir" "corpus"
                   "--var" "cli-filter-tests.a/a-1-test"]))))
    (it "can select multiple vars"
      (expect
        (match?
         {:total 2
          :pass 2
          :fail 0
          :exit 0
          :results {::lr/source-type :lazytest/run
                    :doc nil?
                    :children [{::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.a
                                :children [{::lr/source-type :lazytest/var
                                            :doc "a-1-test"}]}
                               {::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.b
                                :children [{::lr/source-type :lazytest/var
                                            :doc "b-2-test"}]}]}}
         (sut/run ["--output" "quiet"
                   "--dir" "corpus"
                   "--var" "cli-filter-tests.a/a-1-test"
                   "--var" "cli-filter-tests.b/b-2-test"])))))
  (it "handles mismatched var and namespace filters"
    (expect
      (match?
       {:total 4
        :pass 4
        :fail 0
        :exit 0
        :results {::lr/source-type :lazytest/run
                  :doc nil?
                  :children [{::lr/source-type :lazytest/ns
                              :doc 'cli-filter-tests.a
                              :children [{::lr/source-type :lazytest/var
                                          :doc "a-1-test"}
                                         {::lr/source-type :lazytest/var
                                          :doc "a-2-test"}
                                         {::lr/source-type :lazytest/var
                                          :doc "a-3-test"}]}
                             {::lr/source-type :lazytest/ns
                              :doc 'cli-filter-tests.b
                              :children [{::lr/source-type :lazytest/var
                                          :doc "b-2-test"}]}]}}
       (sut/run ["--output" "quiet"
                 "--dir" "corpus"
                 "--namespace" "cli-filter-tests.a"
                 "--var" "cli-filter-tests.b/b-2-test"]))))
  (describe "--include"
    (it "selects by metadata on var"
      (expect
        (match?
         {:total 1
          :pass 1
          :fail 0
          :exit 0
          :results {::lr/source-type :lazytest/run
                    :children [{::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.a
                                :children [{::lr/source-type :lazytest/var
                                            :doc "a-1-test"}]}]}}
         (sut/run ["--output" "quiet"
                   "--dir" "corpus/cli_filter_tests"
                   "--include" "on-var"]))))
    (it "selects by metadata in attr-map"
      (expect
        (match?
         {:total 1
          :pass 1
          :fail 0
          :exit 0
          :results {::lr/source-type :lazytest/run
                    :children [{::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.a
                                :children [{::lr/source-type :lazytest/var
                                            :doc "a-2-test"}]}]}}
         (sut/run ["--output" "quiet"
                   "--dir" "corpus/cli_filter_tests"
                   "--include" "in-attr-map"])))))
  (describe "--exclude"
    (it "selects by metadata on var"
      (expect
        (match?
         {:total 5
          :pass 5
          :fail 0
          :exit 0
          :results {::lr/source-type :lazytest/run
                    :children [{::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.a
                                :children [{::lr/source-type :lazytest/var
                                            :doc "a-2-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "a-3-test"}]}
                               {::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.b
                                :children [{::lr/source-type :lazytest/var
                                            :doc "b-1-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "b-2-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "b-3-test"}]}]}}
         (sut/run ["--output" "quiet"
                   "--dir" "corpus/cli_filter_tests"
                   "--exclude" "on-var"]))))
    (it "selects by metadata in attr-map"
      (expect
        (match?
         {:total 5
          :pass 5
          :fail 0
          :exit 0
          :results {::lr/source-type :lazytest/run
                    :children [{::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.a
                                :children [{::lr/source-type :lazytest/var
                                              :doc "a-1-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "a-3-test"}]}
                               {::lr/source-type :lazytest/ns
                                :doc 'cli-filter-tests.b
                                :children [{::lr/source-type :lazytest/var
                                            :doc "b-1-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "b-2-test"}
                                           {::lr/source-type :lazytest/var
                                            :doc "b-3-test"}]}]}}
         (sut/run ["--output" "quiet"
                   "--dir" "corpus/cli_filter_tests"
                   "--exclude" "in-attr-map"]))))))
