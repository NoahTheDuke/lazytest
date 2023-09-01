(ns lazytest.expect.thrown-test
  (:require
    [lazytest.describe :refer [describe it]]
    [lazytest.expect.thrown :refer [throws?]])
  (:import
    [clojure.lang ExceptionInfo]))

(describe throws?-test
  "catches expected throwable class, rethrows other throwables"
  (it "catches expected throwable class"
      (throws? ExceptionInfo #(throw (ex-info "expected exception" {}))))
  (it "catches Throwables"
      (throws? AssertionError #(assert false)))
  (it "fails if function doesn't throw"
      (false? (throws? ExceptionInfo #(do nil))))
  (it "rethrows other errors"
      (try
        (throws? ExceptionInfo #(assert false))
        false
        (catch AssertionError _ true))))
