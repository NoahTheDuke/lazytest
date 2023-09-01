(ns lazytest.expect
  (:import (lazytest ExpectationFailed)))

(defn- function-call?
  "True if form is a list representing a normal function call."
  [form]
  (and (seq? form)
    (let [sym (first form)]
      (and (symbol? sym)
        (let [v (resolve sym)]
          (and (var? v)
            (bound? v)
            (not (:macro (meta v)))
            (let [f (var-get v)]
              (fn? f))))))))

(defmacro base-fields
  "Useful for all expectations. Sets the base
  properties on the ExpectationFailed."
  [&form exp doc & body]
  `(merge '~(meta &form)
          '~(meta exp)
          (select-keys '~(meta &form) [:line :column])
          {:form ~exp
           :file ~*file*
           :ns '~(ns-name *ns*)}
          ~(when doc {:doc doc})
          ~@body))

(defn expect-fn
  [&form docstring expr]
  `(let [doc# ~docstring
         f# ~(first expr)
         args# (list ~@(rest expr))
         result# (apply f# args#)]
     (or result#
         (throw (ExpectationFailed.
                  (base-fields ~&form '~expr ~docstring
                               {:evaluated (list* f# args#)
                                :result result#}))))))

(defn expect-any
  [&form docstring expr]
  `(let [doc# ~docstring
         result# ~expr]
     (or result#
         (throw (ExpectationFailed.
                  (base-fields ~&form '~expr ~docstring
                               {:result result#}))))))

(defmacro expect
  "Evaluates expression. If it returns logical true, returns that
  result. If the expression returns logical false, throws
  lazytest.ExpectationFailed with an attached map describing the
  reason for failure. Metadata on expr and on the 'expect' form
  itself will be merged into the failure map."
  ([expr] (with-meta (list `expect nil expr)
                     (meta &form)))
  ([docstring expr]
   (if (function-call? expr)
     (expect-fn &form docstring expr)
     (expect-any &form docstring expr))))
