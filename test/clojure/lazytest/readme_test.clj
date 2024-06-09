(ns lazytest.readme-test
  (:require
   [lazytest.core :refer [defdescribe describe expect-it given]]))

(defdescribe +-test "with integers"
  (expect-it "computes the sum of 1 and 2"
    (= 3 (+ 1 2)))
  (expect-it "computes the sum of 3 and 4"
    (= 7 (+ 3 4))))

(defdescribe addition-test
  "Addition"
  (describe "of integers"
    (expect-it "computes small sums"
      (= 3 (+ 1 2)))
    (expect-it "computes large sums"
      (= 7000 (+ 3000 4000))))
  (describe "of floats"
    (expect-it "computes small sums"
      (> 0.00001 (abs (- 0.3 (+ 0.1 0.2)))))
    (expect-it "computes large sums"
      (> 0.00001 (abs (- 3000.0 (+ 1000.0 2000.0)))))))

(defdescribe square-root-test "The square root of two"
  (given [root (Math/sqrt 2)]
    (expect-it "is less than two"
      (< root 2))
    (expect-it "is more than one"
      (> root 1))))
