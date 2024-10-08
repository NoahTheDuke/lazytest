(ns lazytest.core
  (:refer-clojure :exclude [test])
  (:require
   [lazytest.context :as ctx]
   [lazytest.malli]
   [lazytest.suite :as suite]
   [lazytest.test-case :as test-case])
  (:import
   (lazytest ExpectationFailed)))

;;; Utilities

(defn ^:no-doc get-arg
  "For internal use only.
  Pops first argument from args if (pred arg) is true.
  Returns a vector [first-arg remaining-args] or [nil args]."
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn ^:no-doc merged-data
  "For internal use only."
  [body form docstring metadata]
  (merge {:doc docstring
          :file *file*
          :ns *ns*}
         (when (empty? body) {:pending true})
         (meta form)
         {:metadata metadata}))

;;; Public API

(defmacro ->ex-failed
  "Useful for all expectations. Sets the base
  properties on the ExpectationFailed."
  ([expr data] `(->ex-failed nil ~expr ~data))
  ([_&form expr data]
   `(let [data# ~data]
      (ExpectationFailed.
        (:message data#)
        (merge ~(meta _&form)
               ~(meta expr)
               {:expected '~expr
                :file ~*file*
                :ns '~(ns-name *ns*)}
               data#)))))

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
               (let [data# (update (ex-data ex#) :message #(or % ~msg-gensym))]
                 (throw (->ex-failed nil data#))))
             (catch Throwable t#
               (throw (->ex-failed ~&form ~expr {:message ~msg-gensym
                                                 :caught t#}))))))))


(defmacro before
  [& body]
  `(let [before-fn# (fn before# [] (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :before] (fnil conj []) before-fn#)
           nil)
       {:before before-fn#})))

(defmacro before-each
  [& body]
  `(let [before-each-fn# (fn before-each# [] (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :before-each] (fnil conj []) before-each-fn#)
           nil)
       {:before-each before-each-fn#})))

(defmacro after-each
  [& body]
  `(let [after-each-fn# (fn after-each# [] (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :after-each] (fnil conj []) after-each-fn#)
           nil)
       {:after-each after-each-fn#})))

(defmacro after
  "Returns a context whose teardown method evaluates body."
  [& body]
  `(let [after-fn# (fn after# [] (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :after] (fnil conj []) after-fn#)
           nil)
       {:after after-fn#})))

(defmacro around
  "Builds a function for the `around` context.

  Usage:
  (describe some-func
    {:context [(around [f]
                 (binding [*foo* 100]
                   (f)))]}
    ...)"
  [param & body]
  (assert (and (vector? param)
               (= 1 (count param))
               (simple-symbol? (first param))) "Must be a vector of one symbol")
  `(let [around-fn# (fn around# ~param (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :around] (fnil conj []) around-fn#)
           nil)
       {:around around-fn#})))

(defn set-ns-context!
  "Must be a sequence of context maps, presumably built with the appropriate macros."
  [context]
  (alter-meta! *ns* assoc :context (ctx/merge-context context)))

(def ^:dynamic *context* nil)

(defmacro describe
  "Defines a suite of tests.

  doc is a documentation string expression. If the expression is
  a local, it will be used as is. If the expression is a resolvable
  symbol, it will be resolved and used as a var. Otherwise, it's
  evaluated like normal.

  attr-map (optional) is a metadata map.

  children are test cases or nested test suites."
  {:arglists '([doc & children]
               [doc attr-map? & children])}
  [doc & body]
  (let [doc (if (symbol? doc)
              (if (contains? &env doc)
                doc
                (or (resolve doc)
                    doc))
              doc)
        [attr-map children] (get-arg map? body)
        data (merged-data children &form doc (dissoc attr-map :context))
        context (:context attr-map)]
    `(let [suite# (binding [*context* (atom (suite/suite ~data))]
                    (let [ctx-fns# (binding [*context* nil] ~context)]
                      (assert (or (nil? ctx-fns#) (sequential? ctx-fns#))
                              ":context must be a sequence")
                      (swap! *context* update :context
                             (fn [c#]
                               (ctx/merge-context (cons c# ctx-fns#)))))
                    (run! #(if (sequential? %) (doall %) %) (flatten [~@children]))
                    @*context*)]
       (if *context*
         (do (swap! *context* update :children conj suite#)
             nil)
         suite#))))

(defmacro defdescribe
  "`describe` helper that assigns a `describe` call to a Var of the given name.

  test-name is a symbol.

  doc (optional) is a documentation string. Unlike the other helpers,
  this doc must be a string literal.

  attr-map (optional) is a metadata map.

  children are test cases (see 'it') or nested test suites (see 'describe')."
  {:arglists '([test-name & children]
               [test-name doc? attr-map? & children])}
  [test-name & body]
  (assert (symbol? test-name) (str "test-name must be a symbol, given " (pr-str test-name)))
  (let [[doc body] (get-arg string? body)
        [attr-map body] (get-arg map? body)
        test-var (list 'var (symbol (str *ns*) (str test-name)))
        attr-map (-> (meta test-name) 
                     (dissoc :doc)
                     (merge attr-map)
                     (assoc :var test-var))
        body (cond-> [(or doc (str test-name))]
               attr-map (conj attr-map)
               body (concat body))]
    `(def ~(vary-meta test-name assoc :type :lazytest/var)
       (fn ~(gensym (str test-name "_")) []
         (assoc (describe ~@body) :type :lazytest/var)))))

(defmacro given
  "DEPRECATED: No longer needed. Use a normal `let`, please.

  Alias for 'let'."
  {:deprecated "1.0.0"}
  [bindings & body]
  `(let ~bindings
     ~@body))

(defmacro it
  "Defines a single test case that may execute arbitrary code.

  doc is a documentation string expression. If the expression is
  a local, it will be used as is. If the expression is a resolvable
  symbol, it will be resolved and used as a var. Otherwise, it's
  evaluated like normal.

  attr-map (optional) is a metadata map

  body is any code, which must throw an exception (such as with
  'expect') to indicate failure. If the code completes without
  throwing any exceptions, the test case has passed.

  NOTE: Because failure requires an exception, no assertions after
  the thrown exception will be run."
  {:arglists '([doc & body]
               [doc|sym? attr-map? & body])}
  [doc & body]
  (assert (pos? (count body)) "Must provide body")
  (let [doc (if (symbol? doc)
              (if (contains? &env doc)
                doc
                (or (resolve doc) doc))
              doc)
        [attr-map body] (get-arg map? body)
        metadata (merged-data body &form doc (dissoc attr-map :context))
        context (:context attr-map)]
    `(let [test-case# (test-case/test-case
                       (assoc ~metadata
                              :body (fn it# [] (let [ret# (do ~@body)] ret#))
                              :context (binding [*context* nil]
                                         (ctx/merge-context ~context))))]
       (if *context*
         (swap! *context* update :children conj test-case#)
         test-case#))))

(defmacro expect-it
  "Defines a single test case that wraps the given expr in an `expect` call.

  doc is a documentation string expression. If the expression is
  a local, it will be used as is. If the expression is a resolvable
  symbol, it will be resolved and used as a var. Otherwise, it's
  evaluated like normal.

  attr-map (optional) is a metadata map

  expr is a single expression, which must return logical true to
  indicate the test case passes or logical false to indicate failure."
  {:arglists '([doc expr]
               [doc|sym? attr-map? expr])}
  [doc & body]
  (let [doc (if (symbol? doc)
              (if (contains? &env doc)
                doc
                (or (resolve doc)
                    doc))
              doc)
        [attr-map exprs] (get-arg map? body)
        [assertion] exprs
        metadata (merged-data body &form doc attr-map)]
    (when (not= 1 (count exprs))
      (throw (IllegalArgumentException. "expect-it takes 1 expr")))
    (when (and (seq? assertion) (symbol? (first assertion)))
      (assert (not= "expect" (name (first assertion)))))
    `(let [test-case# (test-case/test-case
                       (assoc ~metadata :body (fn expect-it# [] (expect ~assertion ~doc))))]
       (if *context*
         (swap! *context* update :children conj test-case#)
         test-case#))))

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
         (if (some (fn causes-with-msg?-wrapper [cause]
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

;;;; Aliases

(defmacro context
  "Alias of [[describe]]."
  {:arglists '([doc & children]
               [doc attr-map? & children])}
  [doc & body]
  (with-meta `(describe ~doc ~@body) (meta &form)))

(defmacro specify
  "Alias of [[it]]."
  {:arglists '([doc & body]
               [doc|sym? attr-map? & body])}
  [doc & body]
  (with-meta `(it ~doc ~@body) (meta &form)))

(defmacro should
  "Alias of [[expect]]."
  ([expr]
   (with-meta `(expect ~expr nil) (meta &form)))
  ([expr msg]
   (with-meta `(expect ~expr ~msg) (meta &form))))
