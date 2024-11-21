;; copyright (c) 2018-2024 sean corfield, all rights reserved

(ns lazytest.extensions.expectations
  "Adapts Expectations v2 (https://github.com/clojure-expectations/clojure-test) to Lazytest."
  {:clj-kondo/ignore [:unused-binding]}
  (:require
   [lazytest.core :as lt]
   [clojure.data :as data]
   [clojure.string :as str]
   #?(:cljs [planck.core])
   ; #?(:clj  [clojure.test :as t]
   ;          :cljs [cljs.test :include-macros true :as t])
   #?(:clj  [clojure.spec.alpha :as s])
   #?(:cljs [cljs.spec.alpha :as s])
   #?@(:cljs [pjstadig.humane-test-output
              pjstadig.print
              pjstadig.util])))

(def humane-test-output?
  "If Humane Test Output is available, activate it, and enable compatibility
  of our `=?` with it.

  This Var will be `true` if Humane Test Output is available and activated,
  otherwise it will be `nil`."
  #?(:clj (try (require 'pjstadig.humane-test-output)
               ((resolve 'pjstadig.humane-test-output/activate!))
               true
               (catch Exception _))
     :cljs (do
             (defmethod cljs.test/report [:cljs.test/default :fail]
               [event]
               (#'pjstadig.util/report-
                 (if (:diffs event)
                   event
                   (pjstadig.print/convert-event event))))
             ; This should be true for normal operation, false for testing
             ; this framework and running the :negative tests.
             true)))

(defn ^:no-doc illegal-argument [s]
  (#?(:clj IllegalArgumentException. :cljs js/Error.) s))

;; stub functions for :refer compatibility:
(defn- bad-usage [s]
  `(throw (illegal-argument (str ~s " should only be used inside expect"))))

#_(defmacro =?
  "Internal fuzzy-equality test (clojure.test/assert-expr)."
  [expected actual & [form]]
  (bad-usage "=?"))

(defmacro in
  "`(expect expected (in actual))` -- expect a subset of a collection.

  If `actual` is a hash map, `expected` can be a hash map of key/value pairs
  that you expect to be in the `actual` result (there may be other key/value
  pairs, which are ignored).

  If `actual` is a set, vector, or list, `expected` can be any value that
  you expect to be a member of the `actual` data.

  `(expect {:b 2} (in {:a 1 :b 2 :c 3}))`
  `(expect 2 (in #{1 2 3}))`
  `(expect 2 (in [1 2 3]))`

  `in` may only be used inside `expect` and is a purely syntactic construct.
  This macro can be `refer`'d to satisfy tooling like `clj-kondo`."
  [coll]
  (bad-usage "in"))

(defmacro from-each
  "`(expect expected (from-each [v coll] (f v)))` -- expect this to be true
  for each element of collection. `(f v)` is the actual result.

  Equivalent to: `(doseq [v coll] (expect expected (f v)))`

  `(expect even? (from-each [v (range 10)] (* 2 v)))`

  `from-each` may only be used inside `expect` and is a purely syntactic construct.
  This macro can be `refer`'d to satisfy tooling like `clj-kondo`."
  [bindings & body]
  (bad-usage "from-each"))

(defmacro more-of
  "`(expect (more-of destructuring expected1 actual1 ...) actual)` -- provide
  multiple expectations on `actual` based on binding it against the
  `destructuring` expression (like in a `let`) and then expecting things about
  its subcomponents.

  Equivalent to: `(let [destructuring actual] (expect expected1 actual1) ...)`

  `(expect (more-of [a b] string? a int? b) [\"test\" 42])`

  `more-of` may only be used inside `expect` and is a purely syntactic construct.
  This macro can be `refer`'d to satisfy tooling like `clj-kondo`."
  [destructuring & expected-actual-pairs]
  (bad-usage "more-of"))

(defmacro more->
  "`(expect (more-> expected1 (threaded1) ...) actual)` -- provide multiple
  expectations on `actual` based on threading it into various expressions.

  Equivalent to: `(do (expect expected1 (-> actual (threaded1))) ...)`

  `(expect (more-> string? (first) int? (second)) [\"test\" 42])`

  `more->` may only be used inside `expect` and is a purely syntactic construct.
  This macro can be `refer`'d to satisfy tooling like `clj-kondo`."
  [& expected-threaded-pairs]
  (bad-usage "more->"))

(defmacro more
  "`(expect (more expected1 ...) actual)` -- provide multiple expectations
  on `actual` as a series of expected results.

  Equivalent to: `(do (expect expected1 actual) ...)`

  `(expect (more int? even?) 42)`

  `more` may only be used inside `expect` and is a purely syntactic construct.
  This macro can be `refer`'d to satisfy tooling like `clj-kondo`."
  [& expecteds]
  (bad-usage "more"))

(defn ^:no-doc spec?
  "Detects whether an expected expression seems to be a Spec."
  [e]
  (and (keyword? e) (s/get-spec e)))

(defn ^:no-doc str-match
  "Returns the match off of the beginning of two strings."
  [a b]
  (loop [a-seq (seq a)
         b-seq (seq b)
         match 0]
    (if (= (first a-seq) (first b-seq))
      (recur (next a-seq) (next b-seq) (inc match))
      (subs a 0 match))))

(defn ^:no-doc str-diff
  "Returns three strings [only-in-a only-in-b in-both]"
  [a b]
  (let [match (str-match a b)
        match-len (count match)]
    [(subs a match-len) (subs b match-len) match]))

(defn ^:no-doc str-msg
  "Given output from str-diff, produce a message about the difference."
  [a b in-both]
  (str "matches: " (pr-str in-both)
       "\n>>>  expected diverges: " (pr-str
                                     (clojure.string/replace a in-both ""))
       "\n>>>    actual diverges: " (pr-str
                                     (clojure.string/replace b in-both ""))
       "\n"))

;; smart equality extension to clojure.test assertion -- if the expected form
;; is a predicate (function) then the assertion is equivalent to (is (e a))
;; rather than (is (= e a)) and we need the type check done at runtime, not
;; as part of the macro translation layer
(defmacro =?
  ([e a msg] (with-meta `(=? ~e ~a nil ~msg) (meta &form)))
  ([e a form' msg]
   (let [conform? (boolean (spec? e))]
    `(let [e# ~e
           a# ~a
           f# ~form'
           valid?# (when ~conform? s/valid?)
           explain-str?# (when ~conform? s/explain-str)
           [r# m# ef# af#]
           (cond ~conform?
                 [(valid?# e# a#)
                  (explain-str?# e# a#)
                  (list '~'s/valid? '~e '~a)
                  (list '~'not (list '~'s/valid? '~e a#))]
                 (fn? e#)
                 [(e# a#)
                  (str '~a " did not satisfy " '~e)
                  (list '~e '~a)
                  (list '~'not (list '~e a#))]
                 (isa? (type e#)
                       #?(:clj java.util.regex.Pattern
                          :cljs (type #"regex")))
                 [(some? (re-find e# a#))
                  (str (pr-str a#) " did not match " (pr-str e#) "\n")
                  (list '~'re-find '~e '~a)
                  (list '~'not (list '~'re-find e# a#))]
                 #?(:clj (and (class? e#) (class? a#))
                    :cljs false) ; maybe figure this out later
                 [(isa? a# e#) ; (expect parent child)
                  (str a# " is not derived from " e# "\n")
                  (list '~'isa? '~a '~e)
                  (list '~'not (list '~'isa? a# e#))]
                 #?(:clj (class? e#)
                    :cljs false) ; maybe figure this out later
                 [(instance? e# a#) ; (expect klazz object)
                  (str a#
                       #?(:clj (str " (" (class a#) ")"))
                       " is not an instance of " e# "\n")
                  (list '~'instance? '~e '~a)
                  #?(:clj (class a#))]
                 :else
                 [(= e# a#)
                  (when (and (string? e#) (string? a#) (not= e# a#))
                    (let [[_# _# in-both#] (str-diff e# a#)]
                      (str-msg e# a# in-both#)))
                  (list '~'= '~e '~a)
                  (list '~'not= e# a#)])
           humane?# (and humane-test-output? (not (fn? e#)) (not ~conform?))]
       (or r#
           (throw (lt/->ex-failed
                   ~&form
                   {:message (if m# (if ~msg (str ~msg "\n" m#) m#) ~msg)
                    :diffs (if humane?#
                             [[a# (take 2 (data/diff e# a#))]]
                             [])
                    :expected (cond humane?#
                                    e#
                                    f#
                                    f#
                                    ef#
                                    ef#
                                    :else
                                    '~&form)
                    :actual (cond af#
                                  af#
                                  humane?#
                                  [a#]
                                  :else
                                  (list '~'not (list '~'=? e# a#)))})))
       ))))

(comment
  (data/diff "foo" ["bar"])
  )

(defmacro ^:no-doc ?
  "Wrapper for forms that might throw an exception so exception class names
  can be used as predicates. This is only needed for `more->` so that you can
  thread exceptions into code that can parse information out of them, to be
  used with various expect predicates."
  [form]
  `(try ~form
        (catch #?(:clj Throwable
                  :cljs :default)
          t#
          t#)))

#_(defn ^:no-doc all-report
  "Given an atom in which to accumulate results, return a function that
  can be used in place of `clojure.test/do-report`, which simply remembers
  all the reported results.

  This is used to support the semantics of `expect/in`."
  [store]
  (fn [m]
    (swap! store update (:type m) (fnil conj []) m)))

(defmacro expect
  "Translate Expectations DSL to `lazytest.core` language.

  These are approximate translations for the most basic forms:

  `(expect actual)`               => `(expect actual)`

  `(expect expected actual)`      => `(expect (= expected actual))`

  `(expect predicate actual)`     => `(expect (predicate actual))`

  `(expect regex actual)`         => `(expect (re-find regex actual))`

  `(expect ClassName actual)`     => `(expect (instance? ClassName actual))`

  `(expect ExceptionType actual)` => `(expect (thrown? ExceptionType actual))`

  `(expect spec actual)`          => `(expect (s/valid? spec actual))`

  An optional third argument can be provided: a message to be included
  in the output if the test fails.

  In addition, `actual` can be `(from-each [x coll] (computation-of x))`
  or `(in set-of-results)` or `(in larger-hash-map)`.

  Also, `expect` can be one of `(more predicate1 .. predicateN)`,
  `(more-> exp1 expr1 .. expN exprN)` where `actual` is threaded through
  each expression `exprX` and checked with the expected value `expX`,
  or `(more-of binding exp1 val1 .. expN valN)` where `actual` is
  destructured using the `binding` and then each expected value `expX`
  is used to check each `valX` -- expressions based on symbols in the
  `binding`."
  ([a] (with-meta `(lt/expect ~a nil) (meta &form)))
  ([e a] (with-meta `(expect ~e ~a nil true ~e) (meta &form)))
  ([e a msg] (with-meta `(expect ~e ~a ~msg true ~e) (meta &form)))
  ([e a msg ex? e']
   (let [within (if (and (sequential? e')
                         (symbol? (first e'))
                         (= "expect" (name (first e'))))
                  `(pr-str '~e')
                  `(pr-str (list '~'expect '~e' '~a)))
         msg' `(str/join
                "\n"
                (cond-> []
                  ~msg
                  (conj ~msg)
                  ~(not= e e')
                  (conj (str "  within: " ~within))
                  :else
                  (conj (str (pr-str '~a) "\n"))))]
     (cond
       (and (sequential? a)
            (symbol? (first a))
            (= "from-each" (name (first a))))
       (let [[_ bindings & body] a]
         (if (= 1 (count body))
           `(doseq ~bindings
              (expect ~e ~(first body) ~msg ~ex? ~e))
           `(doseq ~bindings
              (expect ~e (do ~@body) ~msg ~ex? ~e))))

       (and (sequential? a)
            (symbol? (first a))
            (= "in" (name (first a))))
       (let [form `(~'expect ~e ~a)]
         `(let [e#     ~e
                a#     ~(second a)
                not-in# (str '~e " not found in " a#)
                msg#    (if (seq ~msg') (str ~msg' "\n" not-in#) not-in#)]
            (cond (and (set? a#) (set? e#))
                 ;; special case of set in set -- report any elements from
                 ;; expected set that are not in the actual set:
                  #_(t/is (~'=? (clojure.set/difference e# a#) #{} '~form) msg#)
                  (=? (clojure.set/difference e# a#) #{} '~form msg#)
                  (or (sequential? a#) (set? a#))
                  (doseq [a'# a#]
                    (expect e# a'# msg# ~ex? ~form))
                  #_(let [all-reports# (atom nil)
                        one-report# (atom nil)]
                   ;; we accumulate any and all failures and errors but we
                   ;; only accumulate passes if each sequential expectation
                   ;; fully passes (i.e., no failures or errors)
                    (with-redefs [t/do-report (all-report one-report#)]
                      (doseq [a'# a#]
                        (expect e# a'# msg# ~ex? ~form)
                        (if (or (contains? @one-report# :error)
                                (contains? @one-report# :fail))
                          (do
                            (when (contains? @one-report# :fail)
                              (swap! all-reports#
                                     update :fail into (:fail @one-report#)))
                            (when (contains? @one-report# :error)
                              (swap! all-reports#
                                     update :error into (:error @one-report#))))
                          (when (contains? @one-report# :pass)
                            (swap! all-reports#
                                   update :pass into (:pass @one-report#))))
                        (reset! one-report# nil)))

                    (if (contains? @all-reports# :pass)
                     ;; report all the passes (and no failures or errors)
                      (doseq [r# (:pass @all-reports#)] (t/do-report r#))
                      (do
                        (when-let [r# (first (:error @all-reports#))]
                          (t/do-report r#))
                        (when-let [r# (first (:fail @all-reports#))]
                          (t/do-report r#)))))
                  (map? a#)
                  (if (map? e#)
                    (let [submap# (select-keys a# (keys e#))]
                      #_(t/is (~'=? e# submap# '~form) ~msg')
                      (=? e# submap# '~form ~msg'))
                    (throw (illegal-argument "'in' requires map or sequence")))
                  :else
                  (throw (illegal-argument "'in' requires map or sequence")))))

       (and (sequential? e)
            (symbol? (first e))
            (= "more" (name (first e))))
       (let [sa (gensym)
             es (mapv (fn [e] `(expect ~e ~sa ~msg ~ex? ~e')) (rest e))]
         `(let [~sa (? ~a)] ~@es))

       (and (sequential? e)
            (symbol? (first e))
            (= "more->" (name (first e))))
       (let [sa (gensym)
             es (mapv (fn [[e a->]]
                        (if (and (sequential? a->)
                                 (symbol? (first a->))
                                 (let [s (name (first a->))]
                                   (or (str/ends-with? s "->")
                                       (str/ends-with? s "->>"))))
                          `(expect ~e (~(first a->) ~sa ~@(rest a->)) ~msg false ~e')
                          `(expect ~e (-> ~sa ~a->) ~msg false ~e')))
                      (partition 2 (rest e)))]
         `(let [~sa (? ~a)] ~@es))

       (and (sequential? e)
            (symbol? (first e))
            (= "more-of" (name (first e))))
       (let [es (mapv (fn [[e a]] `(expect ~e ~a ~msg ~ex? ~e'))
                      (partition 2 (rest (rest e))))]
         `(let [~(second e) ~a] ~@es))

       #?(:clj (and ex? (symbol? e) (resolve e) (class? (resolve e)))
          :cljs (and ex?
                     (symbol? e)
                     (planck.core/find-var e)
                     (or (= 'js/Error e)
                        ; is it a symbol which is not a predicate?
                         (and (fn? (deref (planck.core/find-var e)))
                              (not= (pr-str (deref (planck.core/find-var e)))
                                    "#object[Function]")))))
       #?(:clj (if (isa? (resolve e) Throwable)
                 #_`(t/is (~'thrown? ~e ~a) ~msg')
                 `(lt/expect (lt/throws? ~e #(do ~a)) ~msg')
                 #_`(t/is (~'=? ~e ~a) ~msg')
                 `(=? ~e ~a ~msg'))
          :cljs (if (= 'js/Error e)
                  `(t/is (~'thrown? ~e ~a) ~msg')
                  `(t/is (~'instance? ~e ~a) ~msg')))

       :else
       #_`(t/is (~'=? ~e ~a) ~msg')
       `(=? ~e ~a ~msg')))))

(comment
  (macroexpand '(expect (more-> 1 :a 2 :b 3 (-> :c :d)) {:a 1 :b 2 :c {:d 4}}))
  (macroexpand '(expect (more-of a 2 a) 4))
  (macroexpand '(expect (more-of {:keys [a b c]} 1 a 2 b 3 c) {:a 1 :b 2 :c 3})))

#_(defn- contains-expect?
  "Given a form, return `true` if it contains any calls to the 'expect' macro.

  As of #28, we also recognize qualified 'expect' calls."
  [e]
  (when (and (coll? e) (not (vector? e)))
    (or (and (symbol? (first e))
             (str/starts-with? (name (first e)) "expect"))
        (some contains-expect? e))))

#_(defmacro defexpect
  "Given a name (a symbol that may include metadata) and a test body,
  produce a standard `clojure.test` test var (using `deftest`).

  `(defexpect name expected actual)` is a special case shorthand for
  `(defexpect name (expect expected actual))` provided as an easy way to migrate
  legacy Expectation tests to the 'clojure.test' compatibility version."
  [n & body]
  (if (and (= (count body) 2)
           (not (some contains-expect? body)))
      ;; treat (defexpect my-name pred (expr)) as a special case
      `(t/deftest ~n (expect ~@body))
    ;; #13 match deftest behavior starting in 2.0.0
    `(t/deftest ~n ~@body)))

#_(defmacro expecting
  "The Expectations version of `clojure.test/testing`."
  [string & body]
  `(t/testing ~string ~@body))

(defmacro side-effects
  "Given a vector of functions to track calls to, execute the body.

  Returns a vector of each set of arguments used in calls to those
  functions. The specified functions will not actually be called:
  only their arguments will be tracked. If you need the call to return
  a specific value, the function can be given as a pair of its name
  and the value you want its call(s) to return. Functions given just
  by name will return `nil`."
  [fn-vec & forms]
  (when-not (vector? fn-vec)
    (throw (illegal-argument "side-effects requires a vector as its first argument")))
  (let [mocks (reduce (fn [m f-spec]
                        (if (vector? f-spec)
                          (assoc m (first f-spec) (second f-spec))
                          (assoc m f-spec nil)))
                      {}
                      fn-vec)
        called-args (gensym "called-args")]
    `(let [~called-args (atom [])]
       (with-redefs ~(reduce-kv (fn [defs f v]
                                  (conj defs
                                        f
                                        `(fn [& args#]
                                           (swap! ~called-args conj args#)
                                           ~v)))
                                []
                                mocks)
         ~@forms)
       @~called-args)))

(defn approximately
  "Given a value and an optional delta (default 0.001), return a predicate
  that expects its argument to be within that delta of the given value."
  ([^double v] (approximately v 0.001))
  ([^double v ^double d]
   (fn [x] (<= (- v (Math/abs d)) x (+ v (Math/abs d))))))

(defn between
  "Given a pair of (numeric) values, return a predicate that expects its
  argument to be be those values or between them -- inclusively."
  [a b]
  (fn [x] (<= a x b)))

(defn between'
  "Given a pair of (numeric) values, return a predicate that expects its
  argument to be (strictly) between those values -- exclusively."
  [a b]
  (fn [x] (< a x b)))

(defn functionally
  "Given a pair of functions, return a custom predicate that checks that they
  return the same result when applied to a value. May optionally accept a
  'difference' function that should accept the result of each function and
  return a string explaininhg how they actually differ.
  For explaining strings, you could use expectations/strings-difference.
  (only when I port it across!)

  Right now this produces pretty awful failure messages. FIXME!"
  ([expected-fn actual-fn]
   (functionally expected-fn actual-fn (constantly "not functionally equivalent")))
  ([expected-fn actual-fn difference-fn]
   (fn [x]
     (let [e-val (expected-fn x)
           a-val (actual-fn x)]
       (lt/expect (= e-val a-val) (difference-fn e-val a-val))))))

; #?(:clj (defn use-fixtures
;          "Wrap test runs in a fixture function to perform setup and
;   teardown. Using a fixture-type of `:each` wraps every test
;   individually, while `:once` wraps the whole run in a single function.
;
;   Like `cljs.test/use-fixtures`, also accepts hash maps with `:before`
;   and/or `:after` keys that specify 0-arity functions to invoke
;   before/after the test/run."
;          [fixture-type & fs]
;          (apply t/use-fixtures fixture-type
;                 (map (fn [f]
;                        (if (map? f)
;                          (fn [t]
;                            (when-let [before (:before f)]
;                              (before))
;                            (t)
;                            (when-let [after (:after f)]
;                              (after)))
;                          f))
;                      fs))))
;
; #?(:cljs (defmacro use-fixtures
;           "Wrap test runs in a fixture function to perform setup and
;   teardown. Using a fixture-type of `:each` wraps every test
;   individually, while `:once` wraps the whole run in a single function.
;
;   Hands off to `cljs.test/use-fixtures`, also accepts hash maps with `:before`
;   and/or `:after` keys that specify 0-arity functions to invoke
;   before/after the test/run."
;           [fixture-type & fs]
;           `(cljs.test/use-fixtures ~fixture-type ~@fs)))
;
; (defn from-clojure-test
;   "Intern the specified symbol from `clojure.test` as a symbol in
;   `expectations.clojure.test` with the same value and metadata."
;   [f]
;   (try
;     (let [tf (symbol #?(:clj "clojure.test"
;                         :cljs "cljs.test")
;                      (name f))
;           v (#?(:clj resolve
;                 :cljs planck.core/find-var)
;              tf)
;           m (meta v)]
;       (#?(:clj intern
;           :cljs planck.core/intern)
;        'expectations.clojure.test
;        (with-meta f
;          (update m
;                  :doc
;                  str
;                  (str #?(:clj "\n\nImported from clojure.test."
;                          :cljs "\n\nImported from cljs.test"))))
;        (deref v)))
;     (catch #?(:clj Throwable
;               :cljs :default) _)))
;
;
; ;; bring over other useful clojure.test functions:
; (doseq [f '[#?@(:clj [run-all-tests run-tests run-test-var test-all-vars test-ns with-test])
;             run-test test-var test-vars]]
;   (from-clojure-test f))
