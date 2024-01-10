(ns lazytest.find
  (:require
    [lazytest.suite :refer [suite suite? test-seq]]
    [lazytest.test-case :refer [test-case?]]
    [malli.experimental :as mx]))

(mx/defn ^:private find-var-test-value
  :- [:maybe [:orn [:suite [:fn suite?]]
              [:test-case [:fn test-case?]]]]
  [this-var :- :lt/var]
  (when (bound? this-var)
    (let [value (var-get this-var)]
      (when (or (suite? value) (test-case? value))
        (vary-meta value assoc :type :lazytest/test-var)))))

(defn- test-seq-for-ns [this-ns]
  (seq (keep find-var-test-value (vals (ns-interns this-ns)))))

(mx/defn find-ns-suite :- [:maybe [:fn suite?]]
  "Returns a test suite for the namespace.

  Returns nil if the namespace has no test suites.

  By default, recurses on all Vars in a namespace looking for values
  for which lazytest.suite/suite? is true. If a namesapce
  has :test-suite metadata, uses that value instead.

  Always returns nil for the clojure.core namespace, to avoid special
  Vars such as *1, *2, *3"
  [n]
  (let [n (the-ns n)]
    (when-not (= (the-ns 'clojure.core) n)
      (or (:test-suite (meta n))
        (when-let [s (test-seq-for-ns n)]
          (vary-meta
            (suite (test-seq s))
            assoc :type :lazytest/ns-suite :ns-name (ns-name n)))))))

(defn- suite-for-namespaces [names]
  (vary-meta (suite (test-seq (keep find-ns-suite names)))
    assoc :type :lazytest/run :doc "Namespaces"))

(defn- all-ns-suite []
  (vary-meta (suite (test-seq (keep find-ns-suite (all-ns))))
    assoc :type :lazytest/run :doc "All Namespaces"))

(defn find-suite
  "Returns test suite containing suites for the given namespaces.
  If no names given, searches all namespaces."
  [& names]
  (if (seq names)
    (suite-for-namespaces names)
    (all-ns-suite)))
