(ns context-tests.use-fixture
  (:require
   [lazytest.core :refer [around defdescribe expect it set-ns-context!]]))

(def use-fixture-state (volatile! []))

(defn vconj! [volatile value]
  (vswap! volatile conj value))

(set-ns-context!
 [(around [f]
    (vconj! use-fixture-state :around-before)
    (f)
    (vconj! use-fixture-state :around-after))])

(defdescribe first-test
  (it "works normally"
    (expect (= 1 1))))

(defdescribe second-test
  (it "also works"
    (expect (= 1 1))))
