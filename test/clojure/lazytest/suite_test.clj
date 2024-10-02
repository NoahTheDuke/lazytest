(ns lazytest.suite-test
  (:require
   [lazytest.core :refer [defdescribe describe expect it]]))

;; manually writing test cases (instead of using `it`)
(defn common-test-cases [x]
  [(when (= x 0)
     (it (str x " equals zero") #(expect (= x 0))))
   (when (= x 1)
     (it (str x " equals one") #(expect (= x 1))))
   (when (= x 2)
     (it (str x " equals two") #(expect (= x 2))))
   (when (= x 3)
     (it (str x " equals three") #(expect (= x 3))))
   (when (= x 4)
     (it (str x " equals four") #(expect (= x 4))))])

;; manually writing a describe var (instead of using `defdescribe`)
(def s1
  (describe "One"
    (common-test-cases 1)))

;; writing a normal defdescribe
(defdescribe s2
  "Two"
  (common-test-cases 2))

;; including all of the above in a distinct test
(defdescribe s3 "Three"
  s1
  s2
  (map common-test-cases (range 3 5)))
