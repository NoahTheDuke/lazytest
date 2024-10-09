(ns lazytest.experimental.interfaces.qunit-test
  (:require
   [lazytest.experimental.interfaces.qunit :refer [assert! module! prep-ns-suite! test!]]))

(prep-ns-suite!)

(test! "initial"
  (assert! (= false (pos? 0))))

(module! "Group A")

(test! "foo"
  (assert! (= true (pos? 1))))

(test! "bar"
  (assert! (= true (pos? 1))))
