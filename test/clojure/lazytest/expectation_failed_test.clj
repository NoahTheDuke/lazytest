(ns lazytest.expectation-failed-test
  (:require
   [lazytest.core :refer [defdescribe expect it]])
  (:import
   clojure.lang.IExceptionInfo
   lazytest.ExpectationFailed))

(defdescribe expectation-failed-test
  (let [ex-failed (ExpectationFailed. {:foo :bar})]
    (it "is an AssertionError"
      (expect (instance? AssertionError ex-failed)))
    (it "is not an ExceptionInfo"
      (expect (not (instance? Exception ex-failed))))
    (it "is an IExceptionInfo"
      (expect (instance? IExceptionInfo ex-failed)))
    (it "has a default message"
      (expect (= "Expectation failed" (ex-message ex-failed)))))
  (it "accepts a nil data map"
    (expect (ExpectationFailed. nil))))
