(ns lazytest.experimental.interfaces.clojure-test
  "EXPERIMENTAL. COULD BE CHANGED AT ANY TIME. Please share usage reports at https://github.com/noahtheduke/lazytest/issues

  An adaption of the built-in `clojure.test` framework. [[testing]] works the same way as `clojure.test/testing`, so it does not support metadata selection like [[lazytest.core/describe]]. [[thrown?]] and [[thrown-with-msg?]] must be required to be used as [[is]] does not support `clojure.test/assert-expr`.

  Supported `clojure.test` vars:
  * [[deftest]]
  * [[testing]]
  * [[is]]
  * [[are]]
  * [[thrown?]]
  * [[thrown-with-msg?]]

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
   [clojure.template :as temp]
   [lazytest.core :refer [defdescribe expect it throws? throws-with-msg?]]))

(def ^:dynamic ^:no-doc *testing-strs*
  "Adapted from `clojure.test/*testing-contexts*`."
  (list))

(defmacro testing
  "Adapted from `clojure.test/testing`."
  [doc & body]
  `(binding [*testing-strs* (cons ~doc *testing-strs*)]
     ~@body))

(defn ^:no-doc testing-str []
  (when (seq *testing-strs*)
    (str/join " " (reverse *testing-strs*))))

(defmacro thrown?
  "Adapted from `clojure.test/thrown?`."
  [c expr]
  `(throws? ~c (fn [] (~expr))))

(defmacro thrown-with-msg?
  "Adapted from `clojure.test/thrown-with-msg?`."
  [c re expr]
  `(throws-with-msg? ~c ~re (fn [] (~expr))))

(defmacro is
  "Adapted from `clojure.test/is`."
  ([form] `(expect ~form (testing-str)))
  ([form msg]
   `(expect ~form (str (testing-str) "\n" ~msg))))

(defmacro are
  "Adapted from `clojure.test/are`."
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
  "Adapted from `clojure.test/deftest`."
  [test-name & body]
  (assert (symbol? test-name) "test-name must be a symbol")
  (with-meta
    `(defdescribe ~test-name
      (it nil ~@body))
    (meta &form)))

