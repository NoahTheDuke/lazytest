(ns lazytest.cli-test
  (:require
   [lazytest.core :refer [defdescribe describe expect it]]))

(set! *warn-on-reflection* true)

;; This exists merely to test the -i and -e flags
(defdescribe metadata-test
  "top true"
  {:top true}
  (describe "middle true"
    {:middle true}
    (it "bottom true"
      {:bottom true}
      (expect (= 2 2)))
    (it "another test"
      (expect (= 3 3))))
  (it "existing"
    (expect (= 4 4))))
