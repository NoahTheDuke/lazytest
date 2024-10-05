(ns lazytest.clojure.test-test
  (:require
   [lazytest.clojure.test :refer [are deftest is testing]]))

(deftest +-test "with integers"
  (is (= 3 (+ 1 2))
      "computes the sum of 1 and 2")
  (is (= 7 (+ 3 4))
      "computes the sum of 3 and 4"))

(deftest addition-test
  "Addition"
  (testing "of integers"
    (is (= 3 (+ 1 2))
        "computes small sums")
    (is (= 7000 (+ 3000 4000))
        "computes large sums")))

(deftest are-test
  (are [x y] (= x y)
    2 (+ 1 1)
    4 (* 2 2)))
