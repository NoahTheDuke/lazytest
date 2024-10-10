(ns lazytest.experimental.interfaces.midje
  "EXPERIMENTAL. COULD BE CHANGED AT ANY TIME. Please share usage reports at <https://github.com/noahtheduke/lazytest/issues>.

  An adaption of the [Midje](https://github.com/marick/midje) framework. Only adapts the [[fact]] and [[facts]] macros. [[fact]] acts like [[lazytest.core/it]] for the purposes of test-cases.

  Unlike Midje, [[facts]] is not an alias for [[fact]], and [[fact]] not support the `(given => expected)` syntax as that's highly complex and outside the scope of this namespace.

  [[prep-ns-suite!]] must be called before [[facts]], as they expect `:lazytest/ns-suite` to point to an existing suite.

  Example:
  ```clojure
  (ns noahtheduke.example-test
    (:require
     [lazytest.experimental.interfaces.midje :refer [prep-ns-suite! facts fact]]))

  (prep-ns-suite!)

  (facts \"a simple top level test\"
    (fact \"a test case\"
      (expect true \"expect works inside\"))
    (facts \"a nested facts call\"
      (fact \"this still works\"
        (expect true))))
  ```
  "
  (:require
   [lazytest.core :refer [describe it]]
   [lazytest.suite :refer [suite]]))

(defn prep-ns-suite!
  "Set the *ns* :lazytest/ns-suite to a fresh suite."
  []
  (alter-meta! *ns* assoc :lazytest/ns-suite (suite {:doc (ns-name *ns*)
                                                     :type :lazytest/ns}))
  nil)

(defmacro facts
  {:arglists '([test-name attr-map? & children]
               [doc attr-map? & children])}
  [doc & body]
  (assert (string? doc) "Must provide a string.")
  `(when-let [suite# (describe ~doc ~@body)]
     (alter-meta! *ns* update-in [:lazytest/ns-suite :children] conj suite#)
     nil))

(defmacro fact
  [doc & body]
  (with-meta `(describe ~doc (it "fact test case" ~@body)) (meta &form)))
