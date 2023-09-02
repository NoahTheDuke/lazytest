(ns lazytest.readme-test
  (:require
    [lazytest.describe :refer [describe it given do-it testing]]
    [lazytest.expect :refer [expect]]))

(describe +-test "with integers"
  (it "computes the sum of 1 and 2"
    (= 4 (+ 1 2)))
  (it "computes the sum of 3 and 4"
    (= 7 (+ 3 4)))
  (it "throws exceptions"
    (/ 1 0)))

(describe addition-test
  "Addition"
  (testing "of integers"
    (it "computes small sums"
      (= 3 (+ 1 2)))
    (it "computes large sums"
      (= 7000 (+ 3000 4000))))
  (testing "of floats"
    (it "computes small sums"
      (> 0.00001 (Math/abs (- 0.3 (+ 0.1 0.2)))))
    (it "computes large sums"
      (> 0.00001 (Math/abs (- 3000.0 (+ 1000.0 2000.0)))))))

(describe square-root-test "The square root of two"
  (given [root (Math/sqrt 2)]
    (it "is less than two"
      (< root 2))
    (it "is more than one"
      (> root 1))))

(describe do-it-test "do-it handles side-effects:"
  (let [state (atom 0)]
    (do-it "arbitrary code"
      (expect (= 4 (+ 2 2)))
      (swap! state inc)
      (it "can even be used in later tests"
        (= 1 @state)))))
