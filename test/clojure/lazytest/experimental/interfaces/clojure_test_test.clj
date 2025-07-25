(ns lazytest.experimental.interfaces.clojure-test-test
  (:require
   [lazytest.core :refer [defdescribe expect it]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.runner :as lr]
   [lazytest.test-utils :refer [with-out-str-data-map]]))

(set! *warn-on-reflection* true)

(in-ns 'clojure-test-temp)
(clojure.core/refer 'clojure.core)
(clojure.core/require
 '[lazytest.experimental.interfaces.clojure-test :refer [deftest are is testing
                                                         thrown? thrown-with-msg?]])

(deftest deftest-test
  (is true "expect works inside")
  (testing "testing works"
    (is (= 7 (+ 3 4)) "is works"))
  (testing "are works"
    (are [x y] (= x y)
      4 (+ 2 2)
      8 (* 2 4)))
  (testing "thrown? works"
    (is (thrown? Exception (throw (Exception. "Hello")))))
  (testing "thrown-with-msg? works"
    (is (thrown-with-msg? Exception #".ell." (throw (Exception. "Hello"))))))

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
       (existing-tests))))
  (it "correctly executes"
    (expect
      (match?
       {:result
        {:type :lazytest.suite/suite-result
         :source
         {:type :lazytest/run
          :children 
          [{:type :lazytest/var
            :doc "deftest-test"}]}
         :children
         [{:type :lazytest.suite/suite-result
           :doc "deftest-test"
           :children
           [{:type :pass
             :expected nil
             :actual nil}]}]}}
       (with-out-str-data-map (lr/run-test-suite (existing-tests)))))))
