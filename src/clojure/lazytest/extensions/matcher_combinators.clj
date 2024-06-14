(ns lazytest.extensions.matcher-combinators
  (:require
   [lazytest.core :refer [->ex-failed]]
   [matcher-combinators.clj-test :as mc-clj]
   [matcher-combinators.core :as mc]
   [matcher-combinators.result :as-alias result]))

(defmacro match? [matcher actual]
  `(let [matcher# ~matcher
         actual# ~actual]
     (if (mc/matcher? matcher#)
       (let [result# (mc/match matcher# actual#)
             match?# (mc/indicates-match? result#)]
         (or match?#
           (throw (->ex-failed ~&form
                    {:actual (mc-clj/tagged-for-pretty-printing
                               (list '~'not (list 'match? matcher# actual#))
                               result#)})))
         match?#)
       (throw (->ex-failed ~&form
                {:message "The first argument of match? needs to be a matcher (implement the match protocol)"
                 :evaluated (list `match? matcher# actual#)
                 :actual   (list 'not (list `mc/matcher? matcher#))})))))

(defmacro thrown-match?
  ([matcher expr] (with-meta `(~'thrown-match? clojure.lang.ExceptionInfo ~matcher ~expr)
                             (meta &form)))
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
