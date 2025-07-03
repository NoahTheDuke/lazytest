(ns lazytest.suite-test
  (:require
   [lazytest.core :refer [defdescribe describe expect]]
   [lazytest.suite :refer [suite]]
   [lazytest.test-case :refer [test-case]]))

(set! *warn-on-reflection* true)

;; manually writing test cases (instead of using `it`)
(defn common-test-cases [x]
  [(when (zero? x)
     (test-case {:doc (str x " equals zero")
                 :body #(expect (= x 1))}))
   (when (= x 1)
     (test-case {:doc (str x " equals one")
                 :body #(expect (= x 1))}))
   (when (= x 2)
     (test-case {:doc (str x " equals two")
                 :body #(expect (= x 2))}))
   (when (= x 3)
     (test-case {:doc (str x " equals three")
                 :body #(expect (= x 3))}))
   (when (= x 4)
     (test-case {:doc (str x " equals four")
                 :body #(expect (= x 4))}))
   (when (= x 5)
     (test-case {:doc (str x " equals five")
                 :body #(expect (= x 5))}))])

;; manually writing a suite (instead of using `describe`)
(def s1
  (suite
    {:doc "One"
     :children (common-test-cases 1)}))

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
  s1
  s2
  (s3)
  (map common-test-cases (range 4 6)))
