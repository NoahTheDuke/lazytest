(ns lazytest.suite
  (:require
   [lazytest.malli]))

(set! *warn-on-reflection* true)

^:clj-reload/keep
(defrecord Suite [type doc tests suites context ns file line var metadata])

(defn suite? "True if x is a test suite."
  [obj]
  (instance? Suite obj))

(defn suite
  [base]
  (-> base
      (update :tests #(or % []))
      (update :suites #(or % []))
      (update :context #(or % {}))
      (cond-> (not (:type base)) (assoc :type :lazytest/suite))
      (map->Suite)))

(defn test-var
  [base]
  (suite (assoc base :type :lazytest/var)))

(defn identifier
  "Get a string representation of the suite. Either the suite's
  :doc, its :ns-name, or the string version of its :var."
  [m]
  (or (:doc m) (:ns-name m) (:var m)))

(defn suite-result
  "Creates a suite result map with keys :source and :children.

  source is the test sequence, with identifying metadata.

  children is a sequence of test results and/or suite results."
  [source children]
  {:type :lazytest.suite/suite-result
   :doc (identifier source)
   :file (:file source)
   :line (:line source)
   :source source
   :children children})

(defn suite-result?
  [x]
  (= ::suite-result (:type x)))
