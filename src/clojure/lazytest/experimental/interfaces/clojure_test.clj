(ns lazytest.experimental.interfaces.clojure-test 
  "EXPERIMENTAL. COULD BE CHANGED AT ANY TIME. Please share usage reports at https://github.com/noahtheduke/lazytest/issues

  An adaption of the built-in `clojure.test` framework. [[testing]] works the same way as [[clojure.test/testing]], so it does not support metadata selection like [[lazytest.core/describe]].

  Supported [[clojure.test]] vars:
  * [[deftest]]
  * [[testing]]
  * [[is]]
  * [[are]]

  Example:
  ```clojure
  (ns noahtheduke.example-test
    (:require
     [lazytest.experimental.interfaces.clojure-test :refer [deftest are is testing]]))

  (deftest deftest-test
    (is true \"expect works inside\")
    (testing \"testing works\"
      (is (= 7 (+ 3 4)) \"is works\"))
    (testing \"are works\"
      (are [x y] (= x y)
        2 (+ 1 1)
        4 (* 2 2))))
  ```
  "
  (:require
    [clojure.string :as str]
    [lazytest.core :refer [expect defdescribe it]]
    [clojure.template :as temp]))

(def ^:dynamic *testing-strs*
  "Adapted from [[clojure.test/*testing-contexts*]]."
  (list))

(defmacro testing
  "Adapted from [[clojure.test/testing]]."
  [doc & body]
  `(binding [*testing-strs* (cons ~doc *testing-strs*)]
     ~@body))

(defn testing-str []
  (when (seq *testing-strs*)
    (str/join " " (reverse *testing-strs*))))

(defmacro is
  "Adapted from [[clojure.test/is]]."
  ([form] `(expect ~form (testing-str)))
  ([form msg]
   `(expect ~form (str (testing-str) "\n" ~msg))))

(defmacro are
  "Adapted from [[clojure.test/are]]."
  [argv expr & args]
  (if (or
       (and (empty? argv) (empty? args))
       (and (pos? (count argv))
            (pos? (count args))
            (zero? (mod (count args) (count argv)))))
    (let [checks (map (fn [a] `(is ~(with-meta (temp/apply-template argv expr a) (meta &form)))) 
                      (partition (count argv) args))]
      `(do ~@checks))
    (throw (IllegalArgumentException. "The number of args doesn't match are's argv."))))

(defmacro deftest
  "Adapted from [[clojure.test/deftest]]."
  [test-name & body]
  (assert (symbol? test-name) "test-name must be a symbol")
  (with-meta
    `(defdescribe ~test-name
      (it nil ~@body))
    (meta &form)))

