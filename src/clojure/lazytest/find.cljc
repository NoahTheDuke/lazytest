(ns lazytest.find
  (:require
   [lazytest.core :refer [describe it]]
   [lazytest.suite :refer [suite suite?]]
   [lazytest.test-case :refer [test-case?]]))

(defn- set-var [value this-var]
  (-> value
      (assoc :type :lazytest/var)
      (assoc :var this-var)))

(defn find-var-test-value
  [the-var]
  (when (bound? the-var)
    (let [value (var-get the-var)
          m (meta the-var)
          test-metadata (:lazytest/test m)]
      (cond
        ;; (defdescribe example ...)
        (= :lazytest/var (:type m))
        (set-var (value) the-var)
        ;; (def example (describe ...))
        (suite? value)
        (set-var value the-var)
        ;; (defn example {:lazytest/test (describe ...)})
        ;; (defn example {:lazytest/test (it ...)})
        (or (suite? test-metadata)
            (test-case? test-metadata))
        (let [new-test (describe the-var)]
          (set-var (update new-test :children conj test-metadata) the-var))
        ;; (defn example {:lazytest/test #(expect ...)})
        (fn? test-metadata)
        (set-var (describe the-var
                   (-> (it "`:lazytest/test` metadata" (test-metadata))
                       (merge (select-keys m [:line :column]))))
                 the-var)))))

(defn- test-suites-for-ns [this-ns]
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
  has :lazytest/ns-suite metadata, uses that value instead.

  Always returns nil for the clojure.core namespace, to avoid special
  Vars such as *1, *2, *3"
  [n]
  (when-not (= (the-ns 'clojure.core) n)
    (let [nmeta (meta n)]
      (or (:test-suite nmeta) ;; deprecated, undocumented
          (:lazytest/ns-suite nmeta)
          (when-let [s (test-suites-for-ns n)]
            (let [focused? (some #(-> % :metadata :focus) s)]
              (-> {:children s
                   :doc (ns-name n)
                   :type :lazytest/ns
                   :metadata (dissoc nmeta :context)
                   :context (:context nmeta)}
                  (cond-> focused? (assoc-in [:metadata :focus] (boolean focused?)))
                  (suite))))))))

(comment
  (find-ns-suite *ns*))

(defn find-suite
  "Returns test suite containing suites for the given namespaces.
  If no names given, searches all namespaces."
  [& names]
  (let [names' (or (seq names) (all-ns))
        nses (mapv the-ns names')
        suites (keep find-ns-suite nses)
        focused? (some #(-> % :metadata :focus) suites)]
    (cond-> (suite {:type :lazytest/run
                    :nses nses
                    :children suites})
      focused? (assoc-in [:metadata :focus] (boolean focused?)))))

(comment
  (load-file "corpus/find_tests/examples.clj")
  (:children (find-suite 'find-tests.examples)))
