(ns lazytest.extensions.expectations
  "Adapts Expectations v2 (https://github.com/clojure-expectations/clojure-test) to Lazytest."
  (:require
   [lazytest.core :as lt]
   [clojure.data :as data]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]))

(def humane-test-output?
  "If Humane Test Output is available, activate it, and enable compatibility
  of our `=?` with it.

  This Var will be `true` if Humane Test Output is available and activated,
  otherwise it will be `nil`."
  (try (require 'pjstadig.humane-test-output)
       ((resolve 'pjstadig.humane-test-output/activate!))
       true
       (catch Exception _)))

;; stub functions for :refer compatibility:
(defn- bad-usage [s]
  `(throw (IllegalArgumentException. (str ~s " should only be used inside expect"))))

#_{:clj-kondo/ignore [:unused-binding]}
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

#_{:clj-kondo/ignore [:unused-binding]}
(defmacro from-each
  "`(expect expected (from-each [v coll] (f v)))` -- expect this to be true
  for each element of collection. `(f v)` is the actual result.

  Equivalent to: `(doseq [v coll] (expect expected (f v)))`

  `(expect even? (from-each [v (range 10)] (* 2 v)))`

  `from-each` may only be used inside `expect` and is a purely syntactic construct.
  This macro can be `refer`'d to satisfy tooling like `clj-kondo`."
  [bindings & body]
  (bad-usage "from-each"))

#_{:clj-kondo/ignore [:unused-binding]}
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

#_{:clj-kondo/ignore [:unused-binding]}
(defmacro more->
  "`(expect (more-> expected1 (threaded1) ...) actual)` -- provide multiple
  expectations on `actual` based on threading it into various expressions.

  Equivalent to: `(do (expect expected1 (-> actual (threaded1))) ...)`

  `(expect (more-> string? (first) int? (second)) [\"test\" 42])`

  `more->` may only be used inside `expect` and is a purely syntactic construct.
  This macro can be `refer`'d to satisfy tooling like `clj-kondo`."
  [& expected-threaded-pairs]
  (bad-usage "more->"))

#_{:clj-kondo/ignore [:unused-binding]}
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
  "Internal fuzzy-equality test (clojure.test/assert-expr)."
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
                        java.util.regex.Pattern)
                  [(some? (re-find e# a#))
                   (str (pr-str a#) " did not match " (pr-str e#))
                   (list '~'re-find '~e '~a)
                   (list '~'not (list '~'re-find e# a#))]
                  (and (class? e#) (class? a#)) ; maybe figure this out later
                  [(isa? a# e#) ; (expect parent child)
                   (str a# " is not derived from " e#)
                   (list '~'isa? '~a '~e)
                   (list '~'not (list '~'isa? a# e#))]
                  (class? e#) ; maybe figure this out later
                  [(instance? e# a#) ; (expect klazz object)
                   (str a#
                        (str " (" (class a#) ")")
                        " is not an instance of " e#)
                   (list '~'instance? '~e '~a)
                   (class a#)]
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
                                   (list '~'not (list '~'=? e# a#)))})))))))

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
        (catch Throwable
          t#
          t#)))

(defn ^:no-doc all-report
  "Given an atom in which to accumulate results, return a function that
  can be used in place of `clojure.test/do-report`, which simply remembers
  all the reported results.

  This is used to support the semantics of `expect/in`."
  [store]
  (fn [m]
    (swap! store update (:type m) (fnil conj []) m)))

(defmacro expect
  "Translate Expectations DSL to `lazytest` language.

  These are approximate translations for the most basic forms:

  `(expect actual)`               => `(is actual)`

  `(expect expected actual)`      => `(is (= expected actual))`

  `(expect predicate actual)`     => `(is (predicate actual))`

  `(expect regex actual)`         => `(is (re-find regex actual))`

  `(expect ClassName actual)`     => `(is (instance? ClassName actual))`

  `(expect ExceptionType actual)` => `(is (thrown? ExceptionType actual))`

  `(expect spec actual)`          => `(is (s/valid? spec actual))`

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
   (let [within (if (and (sequential? e') (= 'expect (first e')))
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
       (and (sequential? a) (= 'from-each (first a)))
       (let [[_ bindings & body] a]
         (if (= 1 (count body))
           `(doseq ~bindings
              (expect ~e ~(first body) ~msg ~ex? ~e))
           `(doseq ~bindings
              (expect ~e (do ~@body) ~msg ~ex? ~e))))

       (and (sequential? a) (= 'in (first a)))
       (let [form `(~'expect ~e ~a)]
         `(let [e#     ~e
                a#     ~(second a)
                not-in# (str '~e " not found in " a#)
                msg#    (if (seq ~msg') (str ~msg' "\n" not-in#) not-in#)]
            (cond ;; special case of set in set -- report any elements from
                  ;; expected set that are not in the actual set:
                  (and (set? a#) (set? e#))
                  (=? (clojure.set/difference e# a#) #{} '~form msg#)
                  ;; Lazytest: Different than expectations, just doseqs over the potential comparisons.
                  (or (sequential? a#) (set? a#))
                  (doseq [a'# a#]
                    (expect e# a'# msg# ~ex? ~form))
                  (map? a#)
                  (if (map? e#)
                    (let [submap# (select-keys a# (keys e#))]
                      (=? e# submap# '~form ~msg'))
                    (throw (IllegalArgumentException. "'in' requires map or sequence")))
                  :else
                  (throw (IllegalArgumentException. "'in' requires map or sequence")))))

       (and (sequential? e) (= 'more (first e)))
       (let [sa (gensym)
             es (mapv (fn [e] `(expect ~e ~sa ~msg ~ex? ~e')) (rest e))]
         `(let [~sa (? ~a)] ~@es))

       (and (sequential? e) (= 'more-> (first e)))
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

       (and (sequential? e) (= 'more-of (first e)))
       (let [es (mapv (fn [[e a]] `(expect ~e ~a ~msg ~ex? ~e'))
                      (partition 2 (rest (rest e))))]
         `(let [~(second e) ~a] ~@es))

       (and ex? (symbol? e) (resolve e) (class? (resolve e)))
       (if (isa? (resolve e) Throwable)
         `(lt/expect (lt/throws? ~e #(do ~a)) ~msg')
         `(=? ~e ~a ~msg'))

       :else
       `(=? ~e ~a ~msg')))))

(comment
  (macroexpand '(expect (more-> 1 :a 2 :b 3 (-> :c :d)) {:a 1 :b 2 :c {:d 4}}))
  (macroexpand '(expect (more-of a 2 a) 4))
  (macroexpand '(expect (more-of {:keys [a b c]} 1 a 2 b 3 c) {:a 1 :b 2 :c 3})))
