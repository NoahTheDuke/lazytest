(ns lazytest.experimental.interfaces.qunit
  "EXPERIMENTAL. COULD BE CHANGED AT ANY TIME. Please share usage reports at <https://github.com/noahtheduke/lazytest/issues>.

  An adaption of the [QUnit](https://qunitjs.com/) framework. Only adapts the unnested style as the nested style can be achieved natively with [[lazytest.core]] macros.

  [[prep-ns-suite!]] must be called before [[module!]] or [[test!]], as they expect `:lazytest/ns-suite` to point to an existing suite.

  Example:

  ```clojure
  (ns noahtheduke.example-test
    (:require
     [lazytest.interfaces.qunit :refer [prep-ns-suite! module! test! assert!]]))

  (prep-ns-suite!)

  (module! \"Group A\")

  (test! \"foo\"
    (assert! (true? (pos? 1)))
    (assert! (false? (pos? 0)) \"Expected to be false\"))
  ```"
  (:require
   [lazytest.core :refer [*context* expect it]]
   [lazytest.suite :refer [suite suite?]]))

(defn prep-ns-suite!
  "Set the *ns* :lazytest/ns-suite to a fresh suite."
  []
  (alter-meta! *ns* assoc :lazytest/ns-suite (suite {:doc (ns-name *ns*)
                                                     :type :lazytest/ns}))
  nil)

(defmacro module!
  "Add a new suite to the *ns* :lazytest/ns-suite."
  [doc]
  `(do (assert (suite? (-> *ns* meta :lazytest/ns-suite)))
       (alter-meta! *ns* update-in [:lazytest/ns-suite :children] conj (suite {:doc ~doc}))
       nil))

(defmacro test!
  "Add a new test case to the current suite."
  [doc & body]
  `(binding [*context* nil]
     (assert (suite? (-> *ns* meta :lazytest/ns-suite)))
     (let [test-case# ~(with-meta `(it ~doc ~@body) (meta &form))]
       (alter-meta! *ns* update-in [:lazytest/ns-suite :children]
                    (fn [children#]
                      (if (seq children#)
                        (update-in children# [(dec (count children#)) :children] conj test-case#)
                        (conj children# test-case#)))))
     nil))

(defmacro assert!
  "Alias of [[lazytest.core/expect]] for the QUnit interface."
  ([expr]
   (with-meta `(expect ~expr nil) (meta &form)))
  ([expr msg]
   (with-meta `(expect ~expr ~msg) (meta &form))))
