(ns examples.suite
  (:require
   [lazytest.core :refer [defdescribe describe expect]]
   [lazytest.suite :refer [suite test-seq]]
   [lazytest.test-case :refer [test-case]]))

;; manually writing test cases (instead of using `it`)
(defn common-test-cases [x]
  [(vary-meta
    (test-case #(expect (= x 1)))
    assoc :doc (str x " equals one"))
   (vary-meta
    (test-case #(expect (= x 2)))
    assoc :doc (str x " equals two"))])

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

;; using the above in a normal `defdescribe`
(defdescribe s3 "Three"
  (map common-test-cases (range 3 5)))
