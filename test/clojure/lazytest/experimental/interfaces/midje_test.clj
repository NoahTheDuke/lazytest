(ns lazytest.experimental.interfaces.midje-test
  (:require
   [lazytest.core :refer [defdescribe expect it]]
   [lazytest.extensions.matcher-combinators :refer [match?]]))

(in-ns 'midje-temp)
(clojure.core/refer 'clojure.core)
(clojure.core/require
 '[lazytest.core :refer [expect]]
 '[lazytest.experimental.interfaces.midje :refer [fact facts prep-ns-suite!]])

(prep-ns-suite!)

(facts "top level facts"
  (fact "must be nested"
    (expect true "expect works inside"))
  (facts "nested facts works"
    (fact "fact works"
      (expect true))))

(facts "another top level fact"
  (fact "with nested fact"
    (expect true "it just works")))

(fact "top level fact"
  (expect true "it works"))

(in-ns 'lazytest.experimental.interfaces.midje-test)

(let [ns-suite (:lazytest/ns-suite (meta (the-ns 'midje-temp)))]
  (defn existing-tests [] ns-suite))

(remove-ns 'midje-temp)

(defdescribe midje-tests
  (it "has the right shape"
    (expect
      (match?
       {:type :lazytest/ns
        :children
        [{:type :lazytest/suite
          :doc "top level facts"
          :children
          [{:type :lazytest/test-case
            :doc "must be nested"}
           {:type :lazytest/suite
            :doc "nested facts works"
            :children
            [{:type :lazytest/test-case
              :doc "fact works"}]}]}
         {:type :lazytest/suite
          :doc "another top level fact"
          :children
          [{:type :lazytest/test-case
            :doc "with nested fact"}]}
         {:type :lazytest/test-case
          :doc "top level fact"}]}
       (existing-tests)))))
