(ns find-tests.examples 
  (:require
   [lazytest.core :refer [describe expect it]]
   [lazytest.suite :refer [suite test-seq]]))

(defn test-fn
  {:test #(expect (= 0 (test-fn 1)))}
  [a]
  (+ a a))

(defn test-test-case
  {:test (it "test case example"
           (expect (= 1 (test-test-case 1))))}
  [a]
  (+ a a))

(defn test-suite
  {:test (suite
           (test-seq
            [(it "test-seq example" (expect (= 1 (test-suite 1))))
             (it "test-seq example two" (expect (= 0 (test-suite 1))))]))}
  [a]
  (+ a a))

(defn test-describe
  {:test (describe "top level"
           (it "test-describe example" (expect (= 1 (test-describe 1))))
           (it "test-describe example two" (expect (= 0 (test-describe 1)))))}
  [a]
  (+ a a))
