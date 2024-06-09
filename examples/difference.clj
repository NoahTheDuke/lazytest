(ns examples.difference
  (:require
    [lazytest.core :refer [defdescribe expect-it describe]]))

(defdescribe differences-test
  (describe "Differences among"
    (expect-it "simple values"
      (= 1 2))
    (expect-it "simple computed values"
      (= 5 (+ 2 2)))
    (expect-it "simple strings with common prefix"
      (= "foobar" "fooquux"))
    (expect-it "vectors"
      (= [1 2 3 4] [1 2 :a 4]))
    (expect-it "vectors of different lengths"
      (= [1 2 3] [1]))
    (expect-it "vectors of different lengths"
      (= [1] [1 1 3]))
    (expect-it "maps with similar keys"
      (= {:a 1} {:a 2}))
    (expect-it "maps with different keys"
      (= {:a 1} {:b 2}))
    (expect-it "sets"
      (= #{1 2 3} #{2 3 4}))
    (expect-it "seqs"
      (seq []))))
