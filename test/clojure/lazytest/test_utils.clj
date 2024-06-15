(ns lazytest.test-utils
  (:require
    [lazytest.color :as lc]))

(defmacro with-out-str-no-color
  "Evaluates exprs in a context in which *out* is bound to a fresh StringWriter and lazytest.color/*color* is bound to false.  Returns the string created by any nested printing calls or nil if the string is empty."
  {:added "1.0"}
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#
               lc/*color* false]
       ~@body
       (not-empty (str s#)))))
