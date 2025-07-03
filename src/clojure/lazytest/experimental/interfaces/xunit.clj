(ns lazytest.experimental.interfaces.xunit
  "EXPERIMENTAL. COULD BE CHANGED AT ANY TIME. Please share usage reports at <https://github.com/noahtheduke/lazytest/issues>.

  An adaption of the [xUnit](https://en.wikipedia.org/wiki/XUnit) style of test frameworks. A fairly simple aliasing of the [[lazytest.core]] macros:

  * [[lazytest.core/defdescribe]] -> [[defsuite]]
  * [[lazytest.core/describe]] -> [[suite]]
  * [[lazytest.core/it]] -> [[test-case]]
  * [[lazytest.core/expect]] -> [[assert!]]

  Example:

  ```clojure
  (ns noahtheduke.example-test
    (:require
     [lazytest.experimental.interfaces.xunit :refer [defsuite suite test-case assert!]]))

  (defsuite defsuite-test
    (suite \"defsuite works\"
      (assert! true \"expect works inside\"))
    (suite \"suite works\"
      (test-case \"test-case works\"
        (assert! true))))
  ```"
  (:require
   [lazytest.core :refer [defdescribe describe expect it]]))

(set! *warn-on-reflection* true)

(defmacro defsuite
  "Alias of [[lazytest.core/defdescribe]] for the xUnit interface."
  {:arglists '([test-name & children]
               [test-name doc? attr-map? & children])}
  [test-name & body]
  (with-meta `(defdescribe ~test-name ~@body) (meta &form)))

(defmacro suite
  "Alias of [[lazytest.core/describe]] for the xUnit. Unrelated to [[lazytest.suite/suite]]."
  {:arglists '([doc & children]
               [doc attr-map? & children])}
  [doc & body]
  (with-meta `(describe ~doc ~@body) (meta &form)))

(defmacro test-case
  "Alias of [[lazytest.core/it]] for the xUnit interface."
  {:arglists '([doc & body]
               [doc|sym? attr-map? & body])}
  [doc & body]
  (with-meta `(it ~doc ~@body) (meta &form)))

(defmacro assert!
  "Alias of [[lazytest.core/expect]] for the xUnit interface."
  ([expr]
   (with-meta `(expect ~expr nil) (meta &form)))
  ([expr msg]
   (with-meta `(expect ~expr ~msg) (meta &form))))
