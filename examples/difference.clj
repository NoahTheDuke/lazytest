(ns examples.difference
  (:require
    [lazytest.describe :refer [describe it]]
    [lazytest.expect :refer [expect]]))

(describe differences-test
  (it "Differences among"
    (expect (= 1 2) "simple values")
    (expect (= 5 (+ 2 2)) "simple computed values")
    (expect (= "foobar" "fooquux") "simple strings with common prefix")
    (expect (= [1 2 3 4] [1 2 :a 4]) "vectors")
    (expect (= [1 2 3] [1]) "vectors of different lengths")
    (expect (= [1] [1 1 3]) "vectors of different lengths")
    (expect (= {:a 1} {:a 2}) "maps with similar keys")
    (expect (= {:a 1} {:b 2}) "maps with different keys")
    (expect (= #{1 2 3} #{2 3 4}) "sets")))
