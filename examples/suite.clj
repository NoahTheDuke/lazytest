(ns examples.suite
  (:require
   [lazytest.describe :refer [describe testing]]
   [lazytest.expect :refer [expect]]
   [lazytest.suite :refer [suite test-seq]]
   [lazytest.test-case :refer [test-case]]))

(defn common-test-cases [x]
  [(vary-meta
    (test-case #(expect (= x 1)))
    assoc :doc (str x " equals one"))
   (vary-meta
    (test-case #(expect (= x 2)))
    assoc :doc (str x " equals two"))])

(def s1
  (suite
   (vary-meta
    (test-seq (common-test-cases 1))
    assoc :doc "One")))

(def s2
  (testing "Two"
    (common-test-cases 2)))

(describe s3 "Three"
  (map (fn [tc] (common-test-cases tc)) (range 3 5)))
