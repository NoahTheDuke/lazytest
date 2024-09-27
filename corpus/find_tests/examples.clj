(ns find-tests.examples 
  (:require
   [lazytest.core :refer [describe expect it]]))

(defn test-fn
  {:test #(expect (= 0 (test-fn 1)))}
  [a]
  (+ a a))

(defn test-test-case
  {:test (it "test case example"
           (expect (= 1 (test-test-case 1))))}
  [a]
  (+ a a))

(defn test-describe
  {:test (describe "top level"
           (it "test-describe example" (expect (= 1 (test-describe 1))))
           (it "test-describe example two" (expect (= 0 (test-describe 1)))))}
  [a]
  (+ a a))
