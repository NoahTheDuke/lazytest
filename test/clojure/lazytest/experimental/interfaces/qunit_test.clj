(ns lazytest.experimental.interfaces.qunit-test
  (:require
   [lazytest.core :refer [defdescribe it expect]]
   [lazytest.extensions.matcher-combinators :refer [match?]]))

(in-ns 'qunit-temp)
(clojure.core/refer 'clojure.core)
(clojure.core/require
 '[lazytest.experimental.interfaces.qunit :refer [assert! module! prep-ns-suite! test!]])

(prep-ns-suite!)

(test! "initial"
  (assert! (= false (pos? 0))))

(module! "Group A")

(test! "foo"
  (assert! (= true (pos? 1))))

(test! "bar"
  (assert! (= true (pos? 1))))

(in-ns 'lazytest.experimental.interfaces.qunit-test)

(let [ns-suite (:lazytest/ns-suite (meta (the-ns 'qunit-temp)))]
  (defn existing-tests [] ns-suite))

(remove-ns 'qunit-temp)

(defdescribe qunit-tests
  (it "has the right shape"
    (expect
      (match?
       {:type :lazytest/ns
        :children
        [{:type :lazytest/test-case
          :doc "initial"}
         {:type :lazytest/suite
          :doc "Group A"
          :children
          [{:type :lazytest/test-case
            :doc "foo"}
           {:type :lazytest/test-case
            :doc "bar"}]}]}
       (existing-tests)))))
