(ns lazytest.extensions.matcher-combinators
  "Wrappers around matcher-combinators library functions for use in Lazytest tests. Copies the docstrings from the matcher-combinators vars."
  (:require
   [lazytest.core :refer [->ex-failed]]
   [matcher-combinators.clj-test :as mc-clj]
   [matcher-combinators.core :as mc]
   [matcher-combinators.result :as-alias result]))

(defmacro match?
  "Asserts that the `actual` matches the `expected`, where the `expected` can be a value, a predicate function, or a matcher-combinator.

  ```clojure
  (is (match? [0 1 2] (range 3)))
  (is (match? (complement empty?) (range 3)))
  (is (match? (matcher-combinators.matchers/in-any-order [zero? odd? even?]) (range 3)))
  ```"
  [matcher actual]
  `(let [matcher# ~matcher
         actual# ~actual]
     (if (mc/matcher? matcher#)
       (let [result# (mc/match matcher# actual#)
             match?# (mc/indicates-match? result#)]
         (or match?#
           (throw (->ex-failed ~&form
                    {:actual (mc-clj/tagged-for-pretty-printing
                               (list '~'not (list 'match? matcher# actual#))
                               result#)}))))
       (throw (->ex-failed ~&form
                {:message "The first argument of match? needs to be a matcher (implement the match protocol)"
                 :evaluated (list `match? matcher# actual#)
                 :actual   (list 'not (list `mc/matcher? matcher#))})))))

(defmacro thrown-match?
  "Asserts that evaluating expr throws an exception where the exception's ex-data satisfies the provided matcher."
  ([matcher expr] (with-meta `(~'thrown-match? clojure.lang.ExceptionInfo ~matcher ~expr) (meta &form)))
  ([ex-class matcher expr]
   `(let [matcher# ~matcher]
      (try ~expr
           (throw (->ex-failed ~&form nil))
           (catch ~ex-class ex#
             (let [result# (mc/match ~matcher (ex-data ex#))]
               (or (mc/indicates-match? result#)
                 (throw
                   (->ex-failed
                     ~&form
                     {:actual (mc-clj/tagged-for-pretty-printing
                                (list '~'not (list 'thrown-match? ~ex-class ~matcher '~expr))
                                result#)})))))))))
