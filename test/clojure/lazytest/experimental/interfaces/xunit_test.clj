(ns lazytest.experimental.interfaces.xunit-test 
  (:require
    [lazytest.experimental.interfaces.xunit :refer [defsuite suite test-case assert!]]))

(defsuite defsuite-test
  (suite "defsuite works"
    (assert! true "expect works inside"))
  (suite "suite works"
    (test-case "test-case works"
      (assert! true))))
