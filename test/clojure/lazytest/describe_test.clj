(ns lazytest.describe-test
  (:require
    [lazytest.describe :refer [describe it]]
    [lazytest.expect :refer [expect]])
  (:import
    lazytest.ExpectationFailed))

(describe it-test
  (it "will early exit"
    (try
      ((it "when given multiple expressions"
         (expect (= 1 2))
         (throw (ex-info "never reached" {}))))
      (catch ExpectationFailed e
        (= '(= 1 2) (:form (ex-data e))))))
  (let [state (atom 0)]
    (it "arbitrary code"
      (expect (= 4 (+ 2 2)))
      (swap! state inc)
      (= 1 @state))))

(defn plus [a b] (+ a b))

(describe with-redefs-test
  (it "should be rebound"
    (with-redefs [plus *]
      (expect (= 6 (plus 2 3))))))
