(ns lazytest.describe-test
  (:require
    [lazytest.describe :refer [describe do-it it]]
    [lazytest.expect :refer [expect]]) 
  (:import
    lazytest.ExpectationFailed))

(describe do-it-test
  (it "will early exit"
    (try
      ((do-it "when given multiple expressions"
              (expect (= 1 2))
              (throw (ex-info "never reached" {}))))
      (catch ExpectationFailed e
        (= '(= 1 2) (:form (ex-data e))))))
  (let [state (atom 0)]
    (do-it "arbitrary code"
     (expect (= 4 (+ 2 2)))
     (swap! state inc)
     (it "can even be used in later tests"
      (= 1 @state)))))
