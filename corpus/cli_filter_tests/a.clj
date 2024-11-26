(ns cli-filter-tests.a
  (:require
   [lazytest.core :refer [defdescribe expect it]]))

(defdescribe ^:on-var a-1-test
  (it "works"
    (expect (+ 10 20))))

(defdescribe a-2-test
  {:in-attr-map true}
  (it "works"
    (expect (+ 10 20))))

(defdescribe a-3-test
  (it "works"
    (expect (+ 10 20))))
