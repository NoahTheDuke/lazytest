(ns lazytest.find
  (:require
   [lazytest.core :refer [describe it]]
   [lazytest.malli]
   [lazytest.suite :refer [suite suite? test-seq]]
   [lazytest.test-case :refer [test-case?]]))

(defn- set-var [value this-var]
  (vary-meta value assoc :type :lazytest/test-var :var this-var))

(defn find-var-test-value
  [this-var]
  (when (bound? this-var)
    (let [value (var-get this-var)
          m (meta this-var)
          test-metadata (:test m)]
      (cond
        ;; (defdescribe example ...)
        ;; (def example (suite ...))
        (suite? value)
        (set-var value this-var)
        ;; (defn example {:test (describe ...)})
        ;; (defn example {:test (suite ...)})
        (suite? test-metadata)
        (set-var (describe this-var test-metadata) this-var)
        ;; (defn example {:test (it ...)})
        (test-case? test-metadata)
        (set-var (suite (test-seq [test-metadata])) this-var)
        ;; (defn example {:test #(expect ...)})
        (fn? test-metadata)
        (set-var (suite (test-seq [(it "`:test` metadata" (test-metadata))])) this-var)))))

(defn- test-seq-for-ns [this-ns]
  (->> (ns-interns this-ns)
       (vals)
       (sort-by (comp (juxt :line :column) meta))
       (keep find-var-test-value)
       seq))

(defn find-ns-suite
  "Returns a test suite for the namespace.

  Returns nil if the namespace has no test suites.

  By default, recurses on all Vars in a namespace looking for values
  for which lazytest.suite/suite? is true. If a namesapce
  has :test-suite metadata, uses that value instead.

  Always returns nil for the clojure.core namespace, to avoid special
  Vars such as *1, *2, *3"
  [n]
  (when-not (= (the-ns 'clojure.core) n)
    (or (:test-suite (meta n))
        (when-let [s (test-seq-for-ns n)]
          (vary-meta
            (suite (test-seq s))
            assoc :type :lazytest/ns-suite :ns-name (ns-name n))))))

(defn find-suite
  "Returns test suite containing suites for the given namespaces.
  If no names given, searches all namespaces."
  [& names]
  (let [names (or (seq names) (all-ns))
        nses (mapv the-ns names)]
    (vary-meta (suite (test-seq (keep find-ns-suite nses)))
               assoc :type :lazytest/run :nses nses)))
