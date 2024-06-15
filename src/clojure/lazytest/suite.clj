(ns lazytest.suite 
  (:require
    [malli.experimental :as mx]))

(mx/defn test-seq
  "Adds metadata to sequence s identifying it as a test sequence.

  A test sequence is a sequence of test cases and/or test suites.

  Metadata on the test sequence provides identifying information
  for the test suite, such as :ns-name and :doc."
  [s :- sequential?]
  (vary-meta s assoc ::test-seq true :type :lazytest/test-seq))

(defn test-seq?
  "True if s is a test sequence."
  [s]
  (and (sequential? s) (::test-seq (meta s))))

(defmacro suite
  "Wraps sequence in a function and sets metadata identifying
  it as a test suite."
  [s]
  `(vary-meta (fn [] ~s) assoc ::suite true :type :lazytest/suite))

(defn suite?
  "True if x is a test suite."
  [x]
  (and (fn? x) (::suite (meta x))))

(defn identifier
  "Get a string representation of the suite. Either the suite's
  :doc, its :ns-name, or the string version of its :var."
  [m]
  (or (:doc m) (:ns-name m) (:var m)))

(mx/defn suite-result :- :map
  "Creates a suite result map with keys :source and :children.

  source is the test sequence, with identifying metadata.

  children is a sequence of test results and/or suite results."
  [source :- [:fn test-seq?]
   children :- sequential?]
  (let [{:keys [line file] :as sm} (meta source)
        doc (identifier sm)]
    (with-meta {:type ::suite-result
                :line line :file file :doc doc
                :source source
                :children children}
               {:type ::suite-result})))

(defn suite-result?
  "True if x is a suite result."
  [x]
  (isa? (type x) ::suite-result))

(mx/defn expand-suite :- [:fn test-seq?]
  "Expands a test suite, returning a test sequence. Copies metadata
  from the suite function to the resulting test sequence."
  [ste :- [:fn suite?]]
  (vary-meta (ste) merge (dissoc (meta ste) ::suite)))

(defn expand-tree
  "Recursively expands a tree of nested test suites preserving
  metadata."
  [ste]
  (if (suite? ste)
    (let [test-seq' (expand-suite ste)]
      (with-meta
        (map expand-tree test-seq')
        (meta test-seq')))
    ste))
