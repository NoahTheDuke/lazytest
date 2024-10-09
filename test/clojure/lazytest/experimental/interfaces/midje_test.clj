(ns lazytest.experimental.interfaces.midje-test 
  (:require
   [lazytest.core :refer [expect]]
   [lazytest.experimental.interfaces.midje :refer [facts fact prep-ns-suite!]]))

(prep-ns-suite!)

(facts "top level facts"
  (fact "must be nested"
    (expect true "expect works inside"))
  (facts "nested facts works"
    (fact "fact works"
      (expect true))))

(facts "another top level fact"
  (fact "with nested fact"
    (expect true "it just works")))
