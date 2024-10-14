(ns lazytest.readme-test
  (:require
   [lazytest.core :refer [after around before before-each defdescribe describe
                          expect expect-it it]]))

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
  (let [root (Math/sqrt 2)]
    (it "is less than two"
      (expect (< root 2)))
    (it "is more than one"
      (expect (> root 1)))))

(defdescribe before-and-after-test
  (let [state (volatile! [])]
    (describe "before and after"
      {:context [(before (vswap! state conj :before))
                 (after (vswap! state conj :after))]}
      (expect-it "temp" true))
    (expect-it "has been properly tracked"
      (= [:before :after] @state))))

(defdescribe around-test
  (let [state (volatile! [])]
    (describe "around"
      {:context [(around [f]
                   (vswap! state conj :around-before)
                   (f)
                   (vswap! state conj :around-after))]}
      (expect-it "temp" true))
    (expect-it "correctly ran the whole thing"
      (= [:around-before :around-after] @state))))

(defdescribe each-test
  (let [state (volatile! [])]
    (describe "each examples"
      {:context [(before (vswap! state conj :before))
                 (before-each (vswap! state conj :before-each))]}
      (expect-it "temp" (vswap! state conj :expect-1))
      (expect-it "temp" (vswap! state conj :expect-2)))
    (expect-it "has been properly tracked"
      (= [:before :before-each :expect-1 :before-each :expect-2] @state))))
