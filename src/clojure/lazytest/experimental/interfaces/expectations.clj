(ns lazytest.experimental.interfaces.expectations
  "EXPERIMENTAL. COULD BE CHANGED AT ANY TIME. Please share usage reports at https://github.com/noahtheduke/lazytest/issues

  An adaption of the Clojure library [Expectations v2](https://github.com/clojure-expectations/clojure-test). To mirror how it's built for `clojure.test`, [[defexpect]] wraps the entire test in a single test-case. [[expecting]] works the same way as `clojure.test/testing`, so it does not support metadata selection like [[lazytest.core/describe]]. [[expect]] is implemented in [[lazytest.extensions.expectations/expect]]. None of the utility functions from Expectations v2 are adapted here. They can be found in [[lazytest.extensions.expectations]].

  To use just the `expect` assertion from Expectations v2, please see [[lazytest.extensions.expectations/expect]].

  Supported Expectations vars:
  * [[defexpect]]
  * [[expecting]]
  * [[expect]]
  * [[in]]
  * [[from-each]]
  * [[more]]
  * [[more-of]]
  * [[more->]]

  Example:
  ```clojure
  (ns noahtheduke.example-test
    (:require
     [clojure.spec.alpha :as s]
     [lazytest.experimental.interfaces.expectations :refer [defexpect expecting expect]]))

  (s/def ::pos pos?)
  (defexpect expectations-test
    (expecting \"value\"
      (expect true))
    (expecting \"=\"
      (expect 1 1))
    (expecting \"fn?\"
      (let [i 1] (expect pos? i)))
    (expecting \"regex\"
      (expect #\"hello\" \"hello world\"))
    (expecting \"instance?\"
      (expect String \"hello world\"))
    (expecting \"catch\"
      (expect ExceptionInfo (throw (ex-info \"aw shucks\" {}))))
    (expecting \"spec\"
      (expect ::pos 1)))
  ```
  "
  {:clj-kondo/ignore [:unused-binding]}
  (:require
   [clojure.string :as str]
   [lazytest.core :as lt]
   [lazytest.experimental.interfaces.clojure-test :as lt-ct]
   [lazytest.extensions.expectations :as lt-expect]))

(defmacro in
  "Adapted from `expectations.clojure.test/in`."
  [coll]
  (#'lt-expect/bad-usage "in"))

(defmacro from-each
  "Adapted from `expectations.clojure.test/from-each`."
  [bindings & body]
  (#'lt-expect/bad-usage "from-each"))

(defmacro more-of
  "Adapted from `expectations.clojure.test/more-of`."
  [destructuring & expected-actual-pairs]
  (#'lt-expect/bad-usage "more-of"))

(defmacro more->
  "Adapted from `expectations.clojure.test/more->`."
  [& expected-threaded-pairs]
  (#'lt-expect/bad-usage "more->"))

(defmacro more
  "Adapted from `expectations.clojure.test/more`."
  [& expecteds]
  (#'lt-expect/bad-usage "more"))

(defmacro expect
  "Adapted from `expectations.clojure.test/expect`."
  ([a] (with-meta `(lt/expect ~a (lt-ct/testing-str)) (meta &form)))
  ([e a] (with-meta `(expect ~e ~a nil true ~e) (meta &form)))
  ([e a msg] (with-meta `(expect ~e ~a ~msg true ~e) (meta &form)))
  ([e a msg ex? e']
   (let [msg' `(not-empty
                (str/join
                 "\n"
                 (cond-> []
                   (seq lt-ct/*testing-strs*)
                   (conj (lt-ct/testing-str))
                   ~msg
                   (conj ~msg))))]
     (with-meta `(lt-expect/expect ~e ~a ~msg' true ~e) (meta &form)))))

(defmacro defexpect
  "Adapted from `expectations.clojure.test/defexpect`."
  [n & body]
  (with-meta `(lt/defdescribe ~n (lt/it ~(str n) ~@body)) (meta &form)))

(defmacro expecting
  "Adapted from `expectations.clojure.test/expecting`."
  [doc & body]
  `(lt-ct/testing ~doc ~@body))
