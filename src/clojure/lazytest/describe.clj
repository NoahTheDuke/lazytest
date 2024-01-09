(ns lazytest.describe
  (:require
    [clojure.string :as str]
    [lazytest.suite :refer [suite test-seq]]
    [lazytest.test-case :refer [test-case]]))

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

(defmacro testing
  "Like 'describe' but does not create a Var. Used for nesting test
  suites inside 'describe'.

  sym (optional) is a symbol; if present, it will be resolved in the current namespace
  and prepended to the documentation string.

  doc (optional) is a documentation string.

  attr-map (optional) is a metadata map.

  children are test cases (see 'it') or nested test suites (see 'testing')."
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

(defmacro describe
  "Defines a suite of tests assigned to a Var with the given name.

  test-name is a symbol.

  doc (optional) is a documentation string.

  attr-map (optional) is a metadata map.

  children are test cases (see 'it') or nested test suites (see 'testing')."
  {:arglists '([test-name & children]
               [test-name sym? doc? attr-map? & children])}
  [test-name & body]
  (let [[sym body] (get-arg symbol? body)
        [doc body] (get-arg string? body)
        [attr-map body] (get-arg map? body)
        focus (:focus (meta test-name))
        attr-map (cond-> attr-map
                   focus (assoc :focus focus))
        body (cond-> []
               sym (conj sym)
               doc (conj doc)
               attr-map (conj attr-map)
               body (concat body))]
    `(def ~test-name (testing ~@body))))

(defmacro given
  "Like 'let' but returns the expressions of body in a vector.
  Suitable for nesting inside 'describe' or 'testing'."
  [bindings & body]
  {:pre [(vector? bindings)
         (even? (count bindings))]}
  `(let ~bindings
     [~@body]))

(defmacro it
  "Defines a single test case that may execute arbitrary code.

  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  body is any code, which must throw an exception (such as with
  'expect') to indicate failure. If the code completes without
  throwing any exceptions, the test case has passed.

  NOTE: Because failure requires an exception, no assertions after
  the thrown exception will be run."
  {:arglists '([& body]
               [doc? attr-map? & body])}
  [& body]
  (let [[doc body] (get-arg string? body)
        [attr-map body] (get-arg map? body)
        metadata (merged-metadata body &form doc attr-map)]
    `(test-case (with-meta
                  (fn [] ~@body)
                  ~metadata))))
