(ns lazytest.suite
  (:require
   [lazytest.malli]
   [lazytest.test-case :refer [test-case?]]
   [malli.experimental :as mx]))

(set! *warn-on-reflection* true)

(defn test-seq?
  "True if s is a test sequence."
  [s]
  (and (sequential? s) (::test-seq (meta s))))

(defn suite?
  "True if x is a test suite."
  [x]
  (and (fn? x) (::suite (meta x))))

(defn- right-type? [x]
  (or (nil? x)
      (test-seq? x)
      (suite? x)
      (test-case? x)))

(defn test-seq
  "Adds metadata to sequence s identifying it as a test sequence.

  A test sequence is a sequence of test cases and/or test suites.

  Metadata on the test sequence provides identifying information
  for the test suite, such as :ns-name and :doc."
  [s]
  (when-not (and (sequential? s)
                 (every? right-type? s))
    (throw (ex-info (->> s
                         (remove right-type?)
                         (first)
                         (class)
                         (.getName)
                         (str "Expected test-seq or suite or test-case, received "))
                    {:type ::test-seq
                     :data s})))
  (vary-meta s assoc ::test-seq true :type :lazytest/test-seq))

(defmacro suite
  "Wraps sequence in a function and sets metadata identifying
  it as a test suite."
  [s]
  `(vary-meta (fn suite# []
                (try (test-seq ~s)
                     (catch clojure.lang.ExceptionInfo ex#
                       (if (= ::test-seq (:type (ex-data ex#)))
                         (throw (java.lang.IllegalArgumentException. (str (ex-message ex#))))
                         (throw ex#)))))
              assoc ::suite true :type :lazytest/suite))

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
    (with-meta (assoc sm
                      :type ::suite-result
                      :line line :file file :doc doc
                      :source source
                      :children children)
               {:type ::suite-result})))

(defn suite-result?
  "True if x is a suite result."
  [x]
  (isa? (type x) ::suite-result))
