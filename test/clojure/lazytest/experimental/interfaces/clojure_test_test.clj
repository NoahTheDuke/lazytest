(ns lazytest.experimental.interfaces.clojure-test-test
  (:require
   [lazytest.experimental.interfaces.clojure-test :refer [deftest are is testing]]))

(deftest deftest-test
  (is true "expect works inside")
  (testing "testing works"
    (is (= 7 (+ 3 4)) "is works"))
  (testing "are works"
    (are [x y] (= x y)
      2 (+ 1 1)
      4 (* 2 2))))
