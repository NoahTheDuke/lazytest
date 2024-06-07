(ns lazytest.expect
  (:import (lazytest ExpectationFailed)))

(defmacro base-fields
  "Useful for all expectations. Sets the base
  properties on the ExpectationFailed."
  [&form expr doc & body]
  `(ExpectationFailed.
    (merge ~(meta &form)
           ~(meta expr)
           {:form ~expr
            :file ~*file*
            :ns '~(ns-name *ns*)}
           ~(when doc {:doc doc})
           ~@body)))

(defn expect-any
  [&form expr docstring]
  `(let [doc# ~docstring
         args# ~(if (list? expr) (list* 'list expr) expr)
         result# ~expr]
     (or result#
         (throw (base-fields ~&form '~expr ~docstring
                             {:evaluated args#
                              :result result#})))))

(defmacro expect
  "Evaluates expression. If it returns logical true, returns that
  result. If the expression returns logical false, throws
  lazytest.ExpectationFailed with an attached map describing the
  reason for failure. Metadata on expr and on the 'expect' form
  itself will be merged into the failure map."
  ([expr] (with-meta (list `expect expr nil)
                     (meta &form)))
  ([expr docstring]
   (expect-any &form expr docstring)))
