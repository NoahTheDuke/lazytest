(ns lazytest.experimental.interfaces.clojure-test-test
  (:require
   [lazytest.core :refer [defdescribe expect it]]
   [lazytest.extensions.matcher-combinators :refer [match?]]))

(in-ns 'clojure-test-temp)
(clojure.core/refer 'clojure.core)
(clojure.core/require
 '[lazytest.experimental.interfaces.clojure-test :refer [deftest are is testing]])

(deftest deftest-test
  (is true "expect works inside")
  (testing "testing works"
    (is (= 7 (+ 3 4)) "is works"))
  (testing "are works"
    (are [x y] (= x y)
      2 (+ 1 1)
      4 (* 2 2))))

(in-ns 'lazytest.experimental.interfaces.clojure-test-test)

#_{:clj-kondo/ignore [:unresolved-namespace]}
(let [suite (clojure-test-temp/deftest-test)]
  (defn existing-tests [] suite))

(remove-ns 'clojure-test-temp)

(defdescribe clojure-test-tests
  (it "has the right shape"
    (expect
      (match?
       {:type :lazytest/var
        :doc "deftest-test"
        :children
        [{:type :lazytest/test-case
          :doc nil}]}
       (existing-tests)))))
