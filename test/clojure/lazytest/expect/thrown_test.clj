(ns lazytest.expect.thrown-test
  (:require
    [lazytest.describe :refer [describe it given]]
    [lazytest.expect.thrown :refer [throws?]]) 
  (:import
    [clojure.lang ExceptionInfo]))

(describe throws?-test
  "catches expected throwable class, rethrows other throwables"
  (given [a 1]
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
               (catch AssertionError _ true)))))
