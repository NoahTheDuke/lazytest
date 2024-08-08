(ns filter-tests.a
  (:require
   [lazytest.core :refer [defdescribe expect it]]))

(defdescribe a-1-test
  (it "works"
    (expect (+ 1 1))))

(defdescribe a-2-test
  (it "works"
    (expect (+ 1 1))))

(defdescribe a-3-test
  (it "works"
    (expect (+ 1 1))))
