(ns lazytest.suite)

(defn suite
  "Sets metadata on function f identifying it as a test suite. A test
  suite function must be free of side effects and must return a
  test sequence (see test-seq)."
  [f]
  {:pre [(fn? f)]}
  (vary-meta f #(conj {::suite true :type :lazytest/suite} %)))

(defn suite?
  "True if x is a test suite."
  [x]
  (and (fn? x) (::suite (meta x))))

(defn test-seq
  "Adds metadata to sequence s identifying it as a test sequence.

  A test sequence is a sequence of test cases and/or test suites.

  Metadata on the test sequence provides identifying information
  for the test suite, such as :ns-name and :doc."
  [s]
  {:pre [(sequential? s)]}
  (vary-meta s #(conj {::test-seq true :type :lazytest/test-seq} %)))

(defn test-seq?
  "True if s is a test sequence."
  [s]
  (and (sequential? s) (::test-seq (meta s))))

(defn suite-result
  "Creates a suite result map with keys :source and :children.

  source is the test sequence, with identifying metadata.

  children is a sequence of test results and/or suite results."
  [source children]
  {:pre [(test-seq? source)
         (sequential? children)]}
  (with-meta {:source source :children children}
             {:type ::suite-result}))

(defn suite-result?
  "True if x is a suite result."
  [x]
  (isa? (type x) ::suite-result))

(defn expand-suite
  "Expands a test suite, returning a test sequence. Copies metadata
  from the suite function to the resulting test sequence."
  [ste]
  {:pre [(suite? ste)]
   :post [(test-seq? %)]}
  (vary-meta (ste) merge (dissoc (meta ste) ::suite)))

(defn expand-tree
  "Recursively expands a tree of nested test suites preserving
  metadata."
  [ste]
  (if (suite? ste)
    (let [test-seq (expand-suite ste)]
      (with-meta
        (mapv expand-tree test-seq)
        (meta test-seq)))
    ste))
