(ns lazytest.hooks-test 
  (:require
   [lazytest.config :refer [->config]]
   [lazytest.core :refer [before-each defdescribe describe expect expect-it it
                          specify]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.hooks :as sut :refer [defhook]]
   [lazytest.runner :as runner]
   [lazytest.suite :as s :refer [suite]]
   [lazytest.test-case :as tc]
   [lazytest.test-utils :refer [vconj!]]))

(set! *warn-on-reflection* true)

(defhook example-config-hook
  (config [config _]
    (assoc config ::example true)))

(defdescribe config-hook-test
  (expect-it "correctly calls it"
    (match? {::example true}
      (->config {:hooks [`example-config-hook]}))))

(def state (volatile! []))

(defhook example-running-hook
  (config [_ config] (vconj! state :config)
    (assoc config ::config true))
  (pre-test-run [_ suite] (vconj! state [:pre-test-run (s/identifier suite)])
    (assoc suite ::pre-test-run true))
  (post-test-run [_ suite] (vconj! state [:post-test-run (s/identifier suite)])
    (assoc suite ::post-test-run true))
  (pre-test-suite [_ suite] (vconj! state [:pre-test-suite (s/identifier suite)])
    (assoc suite ::pre-test-suite true))
  (post-test-suite [_ suite] (vconj! state [:post-test-suite (s/identifier suite)])
    (assoc suite ::post-test-suite true))
  (pre-test-case [_ tc] (vconj! state [:pre-test-case (tc/identifier tc)])
    (assoc tc ::pre-test-case true))
  (post-test-case [_ tc] (vconj! state [:post-test-case (tc/identifier tc)])
    (assoc tc ::post-test-case true)))

(defdescribe profiling-test
  (before-each
   (vreset! state []))
  (specify example-running-hook
    (let [test-suite1 (-> (describe "suite 1"
                            (it "test case 1" (expect (= 2 1))))
                          (assoc :type :lazytest/var :var #'profiling-test))
          test-suite2 (-> (describe "suite 2"
                            {:skip true}
                            (it "test case 2" (expect (= 2 1))))
                          (assoc :type :lazytest/var :var #'profiling-test))
          result (try (runner/filter-and-run
                       (suite {:type :lazytest/run
                               :doc "full run"
                               :nses [*ns*]
                               :children
                               [(suite {:type :lazytest/ns
                                        :doc "cool ns"
                                        :children [test-suite1 test-suite2]})]})
                       (->config {:output (constantly nil)
                                  :hooks [`example-running-hook]}))
                      (catch Throwable t (prn t) t))]
      (expect (= [:config
                  [:pre-test-run "full run"]
                  [:pre-test-suite "cool ns"]
                  [:pre-test-suite "suite 1"]
                  [:pre-test-case "test case 1"]
                  [:post-test-case "test case 1"]
                  [:post-test-suite "suite 1"]
                  [:post-test-suite "cool ns"]
                  [:post-test-run "full run"]] @state))
      (expect
        (match? 
         {:type :lazytest.suite/suite-result
          :doc "full run"
          :lazytest.hooks-test/pre-test-run true
          :lazytest.runner/source-type :lazytest/run
          :lazytest.runner/duration number?
          :children
          [{:type :lazytest.suite/suite-result
            :doc "cool ns"
            :lazytest.runner/source-type :lazytest/ns
            :lazytest.hooks-test/pre-test-suite true
            :lazytest.hooks-test/post-test-suite true
            :children
            [{:type :lazytest.suite/suite-result
              :doc "suite 1"
              :context {:before-each [] :after-each []}
              :lazytest.runner/source-type :lazytest/var
              :lazytest.hooks-test/post-test-suite true
              :children
              [{:lazytest.hooks-test/post-test-case true
                :doc "test case 1"}]
              :lazytest.hooks-test/pre-test-suite true}]}]}
         result)))))
