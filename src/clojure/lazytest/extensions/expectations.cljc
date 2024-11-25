;; copyright (c) 2018-2024 sean corfield, all rights reserved

(ns lazytest.extensions.expectations
  "Adapts Expectations v2 (https://github.com/clojure-expectations/clojure-test) to Lazytest."
  {:clj-kondo/ignore [:unused-binding]}
  (:require
   [lazytest.core :as lt]
   [clojure.data :as data]
   [clojure.string :as str]
   #?(:cljs [planck.core])
   #?(:clj  [clojure.spec.alpha :as s]
      :cljs [cljs.spec.alpha :as s])))

(defn ^:no-doc illegal-argument [s]
  (#?(:clj IllegalArgumentException. :cljs js/Error.) s))

;; stub functions for :refer compatibility:
(defn- bad-usage [s]
  `(throw (illegal-argument (str ~s " should only be used inside expect"))))

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

(defn =?-impl
  [{:keys [e e# a a# conform?]}]
  (cond conform?
        [(s/valid? e# a#)
         (s/explain-str e# a#)
         (list 's/valid? e a)
         (list s/valid? e# a#)]
        (fn? e#)
        [(e# a#)
         (str a " did not satisfy " e "\n")
         (list e a)
         (list e# a#)]
        (isa? (type e#)
              #?(:clj java.util.regex.Pattern
                 :cljs (type #"regex")))
        [(some? (re-find e# a#))
         (str (pr-str a#) " did not match " (pr-str e#) "\n")
         (list 're-find e a)
         (list re-find e# a#)]
        #?(:clj (and (class? e#) (class? a#))
           :cljs false) ; maybe figure this out later
        [(isa? a# e#) ; (expect parent child)
         (str a# " is not derived from " e# "\n")
         (list 'isa? a e)
         (list isa? e# a#)]
        #?(:clj (class? e#)
           :cljs false) ; maybe figure this out later
        [(instance? e# a#) ; (expect klazz object)
         (str a#
              #?(:clj (str " (" (class a#) ")"))
              " is not an instance of " e# "\n")
         (list 'instance? e a)
         #?(:clj (list instance? e# (class a#)))]
        :else
        [(= e# a#)
         (when (and (string? e#) (string? a#) (not= e# a#))
           (let [[_# _# in-both#] (str-diff e# a#)]
             (str-msg e# a# in-both#)))
         (list '= e a)
         (list = e# a#)]))

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
           [r# m# ef# af#] (=?-impl {:e '~e :e# e#
                                     :a '~a :a# a#
                                     :conform? ~conform?})]
       (or r#
           (throw (lt/->ex-failed
                   ~&form
                   {:message (if m# (if-let [msg# ~msg] (str msg# "\n" m#) m#) ~msg)
                    :expected (or ef# f#)
                    :evaluated af#
                    :actual r#})))))))

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
                  (pr-str e')
                  (pr-str (list 'expect e' a)))
         msg' `(not-empty
                (str/join
                 "\n"
                 (cond-> []
                   ~msg
                   (conj ~msg)
                   ~(not= e e')
                   (conj ~(str "  within: " within)))))]
     (cond
       (and (sequential? a)
            (symbol? (first a))
            (= "from-each" (name (first a))))
       (let [[_ bindings & body] a]
         (if (= 1 (count body))
           `(doseq ~bindings
              ~(with-meta (list `expect e (first body) msg ex? e)
                 (meta &form)))
           `(doseq ~bindings
              ~(with-meta (list `expect e (cons 'do body) msg ex? e)
                 (meta &form)))))

       (and (sequential? a)
            (symbol? (first a))
            (= "in" (name (first a))))
       (let [form (with-meta `(~'expect ~e ~a) (meta &form))
             e_ (gensym "e_")
             a_ (gensym "a_")
             a'_ (gensym "a'_")
             not-in_ (gensym "not-in_")
             msg_ (gensym "msg_")
             submap_ (gensym "submap_")]
         `(let [~e_     ~e
                ~a_     ~(second a)
                ~not-in_ (str '~e " not found in " ~a_)
                ~msg_    (if (seq ~msg') (str ~msg' "\n" ~not-in_) ~not-in_)]
            (cond (and (set? ~a_) (set? ~e_))
                  ~(with-meta (list `=? (list 'clojure.set/difference e_ a_) #{} (list 'quote form) msg_)
                    (meta &form))
                  (or (sequential? ~a_) (set? ~a_))
                  (doseq [~a'_ ~a_]
                    ~(with-meta (list `expect e_ a'_ msg_ ex? (list 'quote form))
                       (meta &form)))
                  (map? ~a_)
                  (if (map? ~e_)
                    (let [~submap_ (select-keys ~a_ (keys ~e_))]
                      ~(with-meta (list `=? e_ submap_ (list 'quote form) msg')
                         (meta &form)))
                    (throw (illegal-argument "'in' requires map or sequence")))
                  :else
                  (throw (illegal-argument "'in' requires map or sequence")))))

       (and (sequential? e)
            (symbol? (first e))
            (= "more" (name (first e))))
       (let [sa (gensym)
             es (mapv (fn [expected]
                        (with-meta `(expect ~expected ~sa ~msg ~ex? ~e')
                          ;; use more line
                          (meta e)))
                      (rest e))]
         `(let [~sa (? ~a)] ~@es))

       (and (sequential? e)
            (symbol? (first e))
            (= "more->" (name (first e))))
       (let [sa (gensym)
             es (mapv (fn [[expected a->]]
                        (with-meta
                          (if (and (sequential? a->)
                                   (symbol? (first a->))
                                   (let [s (name (first a->))]
                                     (or (str/ends-with? s "->")
                                         (str/ends-with? s "->>"))))
                            `(expect ~expected (~(first a->) ~sa ~@(rest a->)) ~msg false ~e')
                            `(expect ~expected (-> ~sa ~a->) ~msg false ~e'))
                          ;; use more-> line
                          (meta e)))
                      (partition 2 (rest e)))]
         `(let [~sa (? ~a)] ~@es))

       (and (sequential? e)
            (symbol? (first e))
            (= "more-of" (name (first e))))
       (let [es (mapv (fn [[expected actual]]
                        (with-meta `(expect ~expected ~actual ~msg ~ex? ~e')
                          ;; use more-of line
                          (meta e)))
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
       #?(:clj (with-meta
                 (if (isa? (resolve e) Throwable)
                   `(lt/expect (lt/throws? ~e (fn [] ~a)) ~msg')
                   `(=? ~e ~a ~msg'))
                 (meta &form))
          :cljs (if (= 'js/Error e)
                  `(t/is (~'thrown? ~e ~a) ~msg')
                  `(t/is (~'instance? ~e ~a) ~msg')))

       :else
       (with-meta `(=? ~e ~a ~msg') (meta &form))))))

(comment
  (macroexpand '(expect (more-> 1 :a 2 :b 3 (-> :c :d)) {:a 1 :b 2 :c {:d 4}}))
  (macroexpand '(expect (more-of a 2 a) 4))
  (macroexpand '(expect (more-of {:keys [a b c]} 1 a 2 b 3 c) {:a 1 :b 2 :c 3})))

(defn- contains-expect?
  "Given a form, return `true` if it contains any calls to the 'expect' macro.

  As of #28, we also recognize qualified 'expect' calls."
  [e]
  (when (and (coll? e) (not (vector? e)))
    (or (and (symbol? (first e))
             (str/starts-with? (name (first e)) "expect"))
        (some contains-expect? e))))

(defmacro defexpect
  "Given a name (a symbol that may include metadata) and a test body,
  produce a standard `lazytest.core` test var (using `defdescribe`).

  `(defexpect name expected actual)` is a special case shorthand for
  `(defexpect name (expect expected actual))` provided as an easy way to migrate
  legacy Expectation tests to the 'lazytest.core' compatibility version."
  [n & body]
  (with-meta
    (if (and (= (count body) 2)
             (not-any? contains-expect? body))
      ;; treat (defexpect my-name pred (expr)) as a special case
      `(lt/defdescribe ~n (lt/it "" (expect ~@body)))
      ;; #13 match deftest behavior starting in 2.0.0
      `(lt/defdescribe ~n ~@body))
    (meta &form)))

(defmacro expecting
  "The Expectations version of `lazytest.core/describe`."
  [string & body]
  (with-meta `(lt/describe ~string ~@body) (meta &form)))

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
