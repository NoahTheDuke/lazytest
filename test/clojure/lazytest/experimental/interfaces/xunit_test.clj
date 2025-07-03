(ns lazytest.experimental.interfaces.xunit-test
  (:require
   [lazytest.core :refer [defdescribe expect it]]
   [lazytest.extensions.matcher-combinators :refer [match?]]))

(set! *warn-on-reflection* true)

(in-ns 'xunit-temp)
(clojure.core/refer 'clojure.core)
(clojure.core/require
 '[lazytest.experimental.interfaces.xunit :refer [defsuite suite test-case assert!]])

(defsuite defsuite-test
  (suite "defsuite works"
    (assert! true "expect works inside"))
  (suite "suite works"
    (test-case "test-case works"
      (assert! true))))

(in-ns 'lazytest.experimental.interfaces.xunit-test)

#_{:clj-kondo/ignore [:unresolved-namespace]}
(let [suite (xunit-temp/defsuite-test)]
  (defn existing-tests [] suite))

(remove-ns 'xunit-temp)

(defdescribe xunit-tests
  (it "has the right shape"
    (expect
      (match?
       {:type :lazytest/var
        :children
        [{:type :lazytest/suite
          :doc "defsuite works"}
         {:type :lazytest/suite
          :doc "suite works"
          :children
          [{:type :lazytest/test-case
            :doc "test-case works"}]}]}
       (existing-tests)))))
