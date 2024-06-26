(ns lazytest.suite-test
  (:require
   [lazytest.core :refer [defdescribe describe expect]]
   [lazytest.suite :refer [suite test-seq]]
   [lazytest.test-case :refer [test-case]]))

;; manually writing test cases (instead of using `it`)
(defn common-test-cases [x]
  (test-seq
    [(when (= x 0)
       (vary-meta
         (test-case #(expect (= x 0)))
         assoc :doc (str x " equals zero")))
     (when (= x 1)
       (vary-meta
         (test-case #(expect (= x 1)))
         assoc :doc (str x " equals one")))
     (when (= x 2)
       (vary-meta
         (test-case #(expect (= x 2)))
         assoc :doc (str x " equals two")))
     (when (= x 3)
       (vary-meta
         (test-case #(expect (= x 3)))
         assoc :doc (str x " equals three")))
     (when (= x 4)
       (vary-meta
         (test-case #(expect (= x 4)))
         assoc :doc (str x " equals four")))]))

;; manually writing test-seqs
(def s0
  (vary-meta
    (test-seq [(common-test-cases 0)])
    assoc :doc "Zero"))

;; manually writing suites (instead of using `describe`)
(def s1
  (suite
   (vary-meta
    (test-seq (common-test-cases 1))
    assoc :doc "One")))

;; manually writing a describe var (instead of using `defdescribe`)
(def s2
  (describe "Two"
    (common-test-cases 2)))

;; writing a normal defdescribe
(defdescribe s3
  "Three"
  (common-test-cases 3))

;; including all of the above in a distinct test
(defdescribe s4 "Four"
  s3
  (map common-test-cases (range 4 6)))
