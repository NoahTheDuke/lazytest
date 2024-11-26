(ns find-tests.examples 
  (:require
   [lazytest.core :refer [describe expect it]]))

(defn test-fn
  {:lazytest/test #(expect (zero? (test-fn 1)))}
  [a]
  (+ a a))

(defn test-test-case
  {:lazytest/test
   (it "test case example"
     (expect (= 1 (test-test-case 1))))}
  [a]
  (+ a a))

(defn test-describe
  {:lazytest/test
   (describe "top level"
     (it "test-describe example" (expect (= 1 (test-describe 1))))
     (it "test-describe example two" (expect (zero? (test-describe 1)))))}
  [a]
  (+ a a))
