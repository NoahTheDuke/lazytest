(ns lazytest.core
  (:require
    [lazytest.malli]
    [clojure.string :as str]
    [lazytest.suite :refer [suite test-seq]]
    [lazytest.test-case :refer [test-case]])
  (:import (lazytest ExpectationFailed)))

;;; Utilities

(defn- get-arg
  "Pops first argument from args if (pred arg) is true.
  Returns a vector [first-arg remaining-args] or [nil args]."
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn- merged-metadata [body form docstring extra-attr-map]
  (merge {:doc (not-empty docstring)
          :file *file*
          :ns *ns*}
         (when (empty? body) {:pending true})
         (meta form)
         extra-attr-map))

(defn- strcat
  "Concatenate strings, with spaces in between, skipping nil."
  [& args]
  (str/join " " (remove nil? args)))

;;; Public API

(defmacro ->ex-failed
  "Useful for all expectations. Sets the base
  properties on the ExpectationFailed."
  ([expr data] `(->ex-failed nil ~expr ~data))
  ([_form expr data]
   `(ExpectationFailed.
      (merge ~(meta _form)
             ~(meta expr)
             {:expected '~expr
              :file ~*file*
              :ns '~(ns-name *ns*)}
             ~data))))

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

(defn expect-fn
  [expr msg]
  `(let [f# ~(first expr)
         args# (list ~@(rest expr))
         result# (apply f# args#)]
     (or result#
         (throw (->ex-failed ~expr {:message ~msg
                                    :evaluated (list* f# args#)
                                    :actual result#})))))

(defn expect-any
  [expr msg]
  `(let [result# ~expr]
     (or result#
         (throw (->ex-failed ~expr {:message ~msg
                                    :actual result#})))))

(defmacro expect
  "Evaluates expression. If it returns logical true, returns that
  result. If the expression returns logical false, throws
  lazytest.ExpectationFailed with an attached map describing the
  reason for failure. Metadata on expr and on the 'expect' form
  itself will be merged into the failure map."
  ([expr] (with-meta (list `expect expr nil)
                     (meta &form)))
  ([expr msg]
   (let [msg-gensym (gensym)]
     `(let [~msg-gensym ~msg]
        (try ~(if (function-call? expr)
                (expect-fn expr msg-gensym)
                (expect-any expr msg-gensym))
             (catch ExpectationFailed ex#
               (throw (->ex-failed nil (assoc (ex-data ex#)
                                              :expected-message ~msg-gensym))))
             (catch Throwable t#
               (throw (->ex-failed ~&form ~expr {:message ~msg-gensym
                                                 :caught t#}))))))))

(defmacro describe
  "Defines a suite of tests.

  sym (optional) is a symbol; if present, it will be resolved in the current namespace and prepended to the documentation string.

  doc (optional) is a documentation string.

  attr-map (optional) is a metadata map.

  children are test cases or nested test suites."
  {:arglists '([& children]
               [sym? doc? attr-map? & children])}
  [& body]
  (let [[sym body] (get-arg symbol? body)
        [doc body] (get-arg string? body)
        [attr-map children] (get-arg map? body)
        docstring (strcat (when sym (resolve sym)) doc)
        metadata (merged-metadata children &form docstring attr-map)]
    `(suite (test-seq
              (with-meta
                (flatten [~@children])
                ~metadata)))))

(defmacro defdescribe
  "`describe` helper that assigns a `describe` call to a Var of the given name.

  test-name is a symbol.

  sym (optional) is a symbol; if present, it will be resolved in the current namespace and prepended to the documentation string.

  doc (optional) is a documentation string.

  attr-map (optional) is a metadata map.

  children are test cases (see 'it') or nested test suites (see 'describe')."
  {:arglists '([test-name & children]
               [test-name sym? doc? attr-map? & children])}
  [test-name & body]
  (let [[sym body] (get-arg symbol? body)
        [doc body] (get-arg string? body)
        [attr-map body] (get-arg map? body)
        focus (:focus (meta test-name))
        test-var (list 'var (symbol (str *ns*) (str test-name)))
        attr-map (cond-> attr-map
                   true (assoc :var test-var)
                   focus (assoc :focus focus))
        body (cond-> []
               sym (conj sym)
               doc (conj doc)
               attr-map (conj attr-map)
               body (concat body))]
    `(def ~test-name (describe ~@body))))

(defmacro given
  "Like 'let' but returns the expressions of body in a vector.
  Suitable for nesting inside 'describe'."
  [bindings & body]
  `(let ~bindings
     [~@body]))

(defmacro it
  "Defines a single test case that may execute arbitrary code.

  sym (optional) is a symbol; if present, it will be resolved in the current namespace and prepended to the documentation string.
  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  body is any code, which must throw an exception (such as with
  'expect') to indicate failure. If the code completes without
  throwing any exceptions, the test case has passed.

  NOTE: Because failure requires an exception, no assertions after
  the thrown exception will be run."
  {:arglists '([& body]
               [sym? doc? attr-map? & body])}
  [& body]
  (let [[sym body] (get-arg symbol? body)
        [doc body] (get-arg string? body)
        [attr-map body] (get-arg map? body)
        doc (strcat (when sym (resolve sym)) doc)
        metadata (merged-metadata body &form doc attr-map)]
    `(test-case (with-meta
                  (fn [] ~@body)
                  ~metadata))))

(defmacro expect-it
  "Defines a single test case that wraps the given expr in an `expect` call.

  body is: doc? attr-map? expr

  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  expr is a single expression, which must return logical true to
  indicate the test case passes or logical false to indicate failure."
  {:arglists '([expr]
               [doc? attr-map? expr])}
  [& body]
  (let [[doc body] (get-arg string? body)
        [attr-map exprs] (get-arg map? body)
        [assertion] exprs
        metadata (merged-metadata body &form doc attr-map)]
    (when (not= 1 (count exprs))
      (throw (IllegalArgumentException. "expect-it takes 1 expr")))
    (when (and (seq? assertion) (symbol? (first assertion)))
      (assert (not= "expect" (name (first assertion)))))
    `(test-case (with-meta
                  (fn [] (expect ~assertion ~doc))
                  ~metadata))))

(defn throws?
  "Calls f with no arguments; returns true if it throws an instance of
  class c. Any other exception will be re-thrown. Returns false if f
  throws no exceptions.

  Useful in `expect-it` or `expect`."
  [c f]
  (try (f) false
       (catch Throwable t
         (or (instance? c t)
             (throw t)))))

(defn throws-with-msg?
  "Calls f with no arguments; catches exceptions of class c. If the
  message of the caught exception does not match re (with re-find),
  throws ExpectationFailed. Any other exception not of class c will
  be re-thrown. Returns false if f throws no exceptions.

  Useful in `expect-it` or `expect`."
  [c re f]
  (try (f) false
       (catch Throwable t
         (if (instance? c t)
           (re-find re (ex-message t))
           (throw t)))))

(defn cause-seq
  "Given a Throwable, returns a sequence of causes. The first element
  of the sequence is the given throwable itself."
  [throwable]
  (when (instance? Throwable throwable)
    (cons throwable (lazy-seq (cause-seq (ex-cause throwable))))))

(defn causes?
  "Calls f with no arguments; returns true if it throws an exception
  whose cause chain includes an instance of class c. Any other
  exception will be re-thrown. Returns false if f throws no
  exceptions.

  Useful in `expect-it` or `expect`."
  [c f]
  (try (f) false
       (catch Throwable t
         (if (some #(instance? c %) (cause-seq t))
           true
           (throw t)))))

(defn causes-with-msg?
  "Calls f with no arguments; catches exceptions with an instance of
  class c in their cause chain. If the message of the causing
  exception does not match re (with re-find), throws
  ExpectationFailed. Any non-matching exception will be re-thrown.
  Returns false if f throws no exceptions.

  Useful in `expect-it` or `expect`."
  [c re f]
  (try (f) false
       (catch Throwable t
         (if (some (fn [cause]
                     (when
                       (and (instance? c cause)
                            (re-find re (ex-message cause)))
                       c))
                   (cause-seq t))
           true
           (throw t)))))

(defn ok?
  "Calls f with no arguments and discards its return value. Returns
  true if f does not throw any exceptions. Use when checking an expression
  that returns a logical false value.

  Useful in `expect-it` or `expect`."
  [f]
  (f) true)
