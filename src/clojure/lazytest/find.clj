(ns lazytest.find
  (:require
   [lazytest.core :refer [describe it]]
   [lazytest.malli]
   [lazytest.suite :refer [suite suite?]]
   [lazytest.test-case :refer [test-case?]]))

(defn- set-var [value this-var]
  (assoc value :type :lazytest/var :var this-var))

(defn find-var-test-value
  [this-var]
  (when (bound? this-var)
    (let [value (var-get this-var)
          m (meta this-var)
          test-metadata (:test m)]
      (cond
        ;; (defdescribe example ...)
        (= :lazytest/var (:type m))
        (set-var (value) this-var)
        ;; (def example (describe ...))
        (suite? value)
        (set-var value this-var)
        ;; (defn example {:test (describe ...)})
        ;; (defn example {:test (it ...)})
        (or (suite? test-metadata)
            (test-case? test-metadata))
        (let [new-test (describe this-var)]
          (set-var (update new-test :children conj test-metadata) this-var))
        ;; (defn example {:test #(expect ...)})
        (fn? test-metadata)
        (set-var (describe this-var
                   (-> (it "`:test` metadata" (test-metadata))
                       (merge (select-keys m [:line :column]))))
                 this-var)))))

#_(comment
  (defn test-fn
    {:test #(expect (= 0 (test-fn 1)))}
    [a]
    (+ a a))

  (defn test-test-case
    {:test (it "test case example"
             (expect (= 1 (test-test-case 1))))}
    [a]
    (+ a a))

  (defn test-describe
    {:test (describe "top level"
             (it "test-describe example" (expect (= 1 (test-describe 1))))
             (it "test-describe example two" (expect (= 0 (test-describe 1)))))}
    [a]
    (+ a a))

  (def test-describe-def
    (describe "test-def-describe"
      (it "test-def-describe example")
      (it "test-def-describe example two")))

  (find-var-test-value #'test-fn)
  )

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
  has :lazytest/suite metadata, uses that value instead.

  Always returns nil for the clojure.core namespace, to avoid special
  Vars such as *1, *2, *3"
  [n]
  (when-not (= (the-ns 'clojure.core) n)
    (or (:test-suite (meta n)) ;; deprecated, undocumented
        (:lazytest/suite (meta n))
        (when-let [s (test-suites-for-ns n)]
          (let [focused? (some #(-> % :metadata :focus) s)]
            (-> (meta n)
                (assoc :children s)
                (assoc :doc (ns-name n))
                (assoc :type :lazytest/ns)
                (cond-> focused? (assoc-in [:metadata :focus] (boolean focused?)))
                (suite)))))))

(comment
  (find-ns-suite *ns*))

(defn find-suite
  "Returns test suite containing suites for the given namespaces.
  If no names given, searches all namespaces."
  [& names]
  (let [names (or (seq names) (all-ns))
        nses (mapv the-ns names)
        suites (keep find-ns-suite nses)
        focused? (some #(-> % :metadata :focus) suites)]
    (cond-> (suite {:type :lazytest/run
                    :nses nses
                    :children suites})
      focused? (assoc-in [:metadata :focus] (boolean focused?)))))

(comment
  (load-file "corpus/find_tests/examples.clj")
  (:children (find-suite 'find-tests.examples)))
