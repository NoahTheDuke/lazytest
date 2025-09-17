(ns lazytest.expectation-failed-test
  (:require
   [lazytest.core :refer [defdescribe expect it]]
   [lazytest.expectation-failed :refer [->ExpectationFailed]]))

(defdescribe expectation-failed-test
  (let [ex-failed (->ExpectationFailed nil {:foo :bar})]
    (it "is an AssertionError"
      (expect (instance? clojure.lang.ExceptionInfo ex-failed)))
    (it "has a default message"
      (expect (= "Expectation failed" (ex-message ex-failed)))))
  (it "accepts a nil data map"
    (expect (->ExpectationFailed nil))))
