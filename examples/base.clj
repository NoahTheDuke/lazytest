(ns examples.base
  (:require
    [lazytest.describe :refer [describe it testing given]]))

(describe +-example "given any 2 integers"
  (it "computes the sum of 3 and 4"
      (= 7 (+ 3 4)))
  (for [x (range 5) y (range 5)]
    (it "is commutative"
        (= (+ x y) (+ y x))))
  (testing "with integers"
    (it "computes sums of small numbers"
        (= 7 (+ 3 4)))
    (it "computes sums of large numbers"
        (= 7000000 (+ 3000000 4000000))))
  (testing "with floating point"
    (it "computes sums of small numbers"
        (= 0.0000007 (+ 0.0000003 0.0000004)))
    (it "computes sums of large numbers"
        (= 7000000.0 (+ 3000000.0 4000000.0)))))

(describe it-example "The it macro"
  (it "allows arbitrary code"
    (println "Hello, do-it!")
    (println "This test will succeed because it doesn't throw.")))

(describe given-example "The square root of two"
  (given [root (Math/sqrt 2)]
    (it "is less than two"
      (< root 2))
    (it "is more than one"
      (> root 1))))
