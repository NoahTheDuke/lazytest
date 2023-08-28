(ns lazytest.describe
  (:require
    [clojure.string :as str]
    [lazytest.expect :refer [expect]]
    [lazytest.random :as r]
    [lazytest.suite :refer [suite test-seq]]
    [lazytest.test-case :refer [test-case]]))

;;; Utilities

(defn- get-arg
  "Pops first argument from args if (pred arg) is true.
  Returns a vector [this-arg remaining-args] or [nil args]."
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn- merged-metadata [body form docstring extra-attr-map]
  (merge (when (empty? body) {:pending true})
         {:doc docstring, :file *file*, :ns *ns*}
         (meta form)
         extra-attr-map))

(defn- strcat
  "Concatenate strings, with spaces in between, skipping nil."
  [& args]
  (str/join " " (remove nil? args)))

;;; Public API

(defmacro testing
  "Like 'describe' but does not create a Var.  Used for nesting test
  suites inside 'describe'.

  sym (optional) is a symbol; if present, it will be resolved in the current namespace
  and prepended to the documentation string.

  doc (optional) is a documentation string.

  attr-map (optional) is a metadata map.

  children are test cases (see 'it') or nested test suites (see 'testing')."
  {:arglists '([& body]
               [sym & body]
               [sym doc & body]
               [sym doc attr-map & body])}
  [& decl]
  (let [[sym decl] (get-arg symbol? decl)
        [doc decl] (get-arg string? decl)
        [attr-map children] (get-arg map? decl)
        docstring (strcat (when sym (resolve sym)) doc)
        metadata (merged-metadata children &form docstring attr-map)]
    `(suite (fn []
              (test-seq
                (with-meta
                  (flatten (list ~@children))
                  ~metadata))))))

(defmacro describe
  "Defines a suite of tests assigned to a Var with the given name.

  test-name is a symbol. If the symbol resolves, it will be appended with `-test` and
  used as the var's name, and the fully-qualified symbol will be prepended to the
  docstring. If the symbol doesn't resolve, it will be used as is.

  doc (optional) is a documentation string.

  attr-map (optional) is a metadata map.

  children are test cases (see 'it') or nested test suites (see 'testing')."
  {:arglists '([test-name & body]
               [test-name doc & body]
               [test-name doc attr-map & body])}
  [test-name & body]
  (let [resolved-name (when-let [r (resolve test-name)] (symbol r))
        test-name (if (and resolved-name
                           (not (str/ends-with? (str resolved-name) "-test")))
                    (symbol (format "%s-%s-test"
                              (namespace resolved-name)
                              (name resolved-name)))
                    test-name)
        body (if resolved-name (cons resolved-name body) body)]
    `(def ~test-name (testing ~@body))))

(defmacro given
  "Like 'let' but returns the expressions of body in a list.
  Suitable for nesting inside 'describe' or 'testing'."
  [bindings & body]
  {:pre [(vector? bindings)
         (even? (count bindings))]}
  `(let ~bindings
     (list ~@body)))

(defmacro for-any
  "Bindings is a vector of name-value pairs, where the values are
  generator functions such as those in lazytest.random. The number of
  test cases generated depends on the number of bindings and
  lazytest.random/default-test-case-count."
  [bindings & body]
  {:pre [(vector? bindings)
         (even? (count bindings))]}
  (let [c (r/scaled-test-case-count (/ (count bindings) 2) (r/default-test-case-count))
        generated-bindings
        (vec (mapcat (fn [[name generator]]
                       [name `((r/sequence-of ~generator :min ~c :max ~c))])
               (partition 2 bindings)))]
    `(for ~generated-bindings
       (list ~@body))))

(defmacro it
  "Defines a single test case.

  decl is: doc? attr-map? expr

  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  expr is a single expression, which must return logical true to
  indicate the test case passes or logical false to indicate failure."
  [& decl]
  (let [[doc decl] (get-arg string? decl)
        [attr-map body] (get-arg map? decl)
        assertion (first body)
        metadata (merged-metadata body &form doc attr-map)]
    `(test-case (with-meta
                  (fn [] (expect ~assertion))
                  ~metadata))))

(defmacro do-it
  "Defines a single test case that may execute arbitrary code.

  decl is: doc? attr-map? body*

  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  body is any code, which must throw an exception (such as with
  'expect') to indicate failure.  If the code completes without
  throwing any exceptions, the test case has passed."
  [& decl]
  (let [[doc decl] (get-arg string? decl)
        [attr-map body] (get-arg map? decl)
        metadata (merged-metadata body &form doc attr-map)]
    `(test-case (with-meta
                  (fn [] ~@body)
                  ~metadata))))
