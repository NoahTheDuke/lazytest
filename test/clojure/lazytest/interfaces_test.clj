(ns lazytest.interfaces-test
  (:refer-clojure :exclude [test])
  (:require
   [lazytest.core :refer [deftest are is testing defcontext context expect specify defsuite suite test facts fact]]))

(defcontext interface-context-test
  (specify "defcontext works"
    (expect true "expect works inside"))
  (context "context works"
    (specify "specify works"
      (expect true))))

(defsuite interface-tdd-test
  (suite "defsuite works"
    (expect true "expect works inside"))
  (suite "suite works"
    (test "specify works"
      (expect true))))

(deftest interface-deftest-test
  (is true "expect works inside")
  (testing "testing works"
    (is (= 7 (+ 3 4)) "is works"))
  (testing "are works"
    (are [x y] (= x y)
      2 (+ 1 1)
      4 (* 2 2))))

(facts interface-midje-test
  (fact "top-level facts works"
    (expect true "expect works inside"))
  (facts "facts works"
    (fact "fact works"
      (expect true))))
