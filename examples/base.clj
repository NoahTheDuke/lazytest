(ns examples.base
  (:require
    [lazytest.core :refer [defdescribe describe expect-it it given]]
    [clojure.math :as math]))

(defdescribe +-example "given any 2 integers"
  (expect-it "computes the sum of 3 and 4"
    (= 7 (+ 3 4)))
  (for [x (range 5) y (range 5)]
    (expect-it "is commutative"
      (= (+ x y) (+ y x))))
  (describe "with integers"
    (expect-it "computes sums of small numbers"
      (= 7 (+ 3 4)))
    (expect-it "computes sums of large numbers"
      (= 7000000 (+ 3000000 4000000))))
  (describe "with floating point"
    (expect-it "computes sums of small numbers"
      (= 0.0000007 (+ 0.0000003 0.0000004)))
    (expect-it "computes sums of large numbers"
      (= 7000000.0 (+ 3000000.0 4000000.0)))))

(defdescribe it-example "The it macro"
  (it "allows arbitrary code"
    (println "Hello, it!")
    (println "This test will succeed because it doesn't throw.")))

(defdescribe given-example "The square root of two"
  (given [root (math/sqrt 2)]
    (expect-it "is less than two"
      (< root 2))
    (expect-it "is more than one"
      (> root 1))))
