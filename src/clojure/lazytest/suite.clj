(ns lazytest.suite
  (:require
   [lazytest.malli]))

(set! *warn-on-reflection* true)

^:clj-reload/keep
(defrecord Suite [type doc children context ns file line var metadata])

(defn suite? "True if x is a test suite."
  [obj]
  (instance? Suite obj))

(defn suite
  "A suite is a container of children, which may be another suite or a test-case."
  {:arglists '([{:keys [type doc children context ns file line var metadata] :as suite}])}
  [base]
  (-> base
      (update :children #(or % []))
      (update :context #(or % {}))
      (cond-> (not (:type base)) (assoc :type :lazytest/suite))
      (map->Suite)))

(defn test-var
  [base]
  (suite (assoc base :type :lazytest/var)))

(defn identifier
  "Get a string representation of the suite. Either the suite's
  :doc or the string version of its :var."
  [m]
  (or (:doc m) (:var m)))

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
