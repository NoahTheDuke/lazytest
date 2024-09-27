(ns lazytest.core2
  (:require
   [lazytest.config :refer [->config]]
   [lazytest.malli]
   [lazytest.reporters :as r :refer [report nested]]
   [lazytest.test-case :refer [try-test-case]]
   [lazytest.context :as ctx])
  (:import
   (lazytest ExpectationFailed)))

;;; Private Utilities

(defn- get-arg
  "Pops first argument from args if (pred arg) is true.
  Returns a vector [first-arg remaining-args] or [nil args]."
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn- merged-data [body form docstring metadata]
  (merge {:doc docstring
          :file *file*
          :ns *ns*}
         (when (empty? body) {:pending true})
         (meta form)
         {:metadata metadata}))

;;; Public API

(defmacro ->ex-failed
  "Useful for all expectations. Sets the base
  properties on the ExpectationFailed."
  ([expr data] `(->ex-failed nil ~expr ~data))
  ([_form expr data]
   `(let [data# ~data]
      (ExpectationFailed.
        (:message data#)
        (merge ~(meta _form)
               ~(meta expr)
               {:expected '~expr
                :file ~*file*
                :ns '~(ns-name *ns*)}
               data#)))))

(defn- function-call?
  "True if form is a list representing a normal function call."
  [form]
  (and (seq? form)
       (let [sym (first form)]
         (and (symbol? sym)
              (let [v (resolve sym)]
                (and (var? v)
                     (bound? v)
                     (not (:macro (meta v)))
                     (let [f (var-get v)]
                       (fn? f))))))))

(defn expect-fn
  [expr msg]
  `(let [f# ~(first expr)
         args# (list ~@(rest expr))
         result# (apply f# args#)]
     (or result#
         (throw (->ex-failed ~expr {:message ~msg
                                    :evaluated (list* f# args#)
                                    :actual result#})))))

(defn expect-any
  [expr msg]
  `(let [result# ~expr]
     (or result#
         (throw (->ex-failed ~expr {:message ~msg
                                    :actual result#})))))

(defmacro expect
  "Evaluates expression. If it returns logical true, returns that
  result. If the expression returns logical false, throws
  lazytest.ExpectationFailed with an attached map describing the
  reason for failure. Metadata on expr and on the 'expect' form
  itself will be merged into the failure map."
  ([expr] (with-meta (list `expect expr nil)
                     (meta &form)))
  ([expr msg]
   (let [msg-gensym (gensym)]
     `(let [~msg-gensym ~msg]
        (try ~(if (function-call? expr)
                (expect-fn expr msg-gensym)
                (expect-any expr msg-gensym))
             (catch ExpectationFailed ex#
               (let [data# (update (ex-data ex#) :message #(or % ~msg-gensym))]
                 (throw (->ex-failed nil data#))))
             (catch Throwable t#
               (throw (->ex-failed ~&form ~expr {:message ~msg-gensym
                                                 :caught t#}))))))))

;;; Specs

^:clj-reload/keep
(defrecord Suite [type doc tests suites context ns file line var metadata])

(defn suite? [obj]
  (instance? Suite obj))

(defn suite
  [base]
  (-> base
      (update :tests #(or % []))
      (update :suites #(or % []))
      (update :context #(or % {}))
      (assoc :type :lazytest/suite)
      (map->Suite)))

(defn test-var
  [base]
  (assoc (suite base) :type :lazytest/var))

^:clj-reload/keep
(defrecord TestCase [type doc body context ns file line metadata])

(defn test-case? [obj]
  (instance? TestCase obj))

(defn test-case
  [base]
  (map->TestCase (-> base
                     (update :context #(or % {}))
                     (assoc :type :lazytest/test-case))))

(def ^:dynamic *context* nil)

(defmacro describe
  [doc & body]
  (let [doc (if (symbol? doc)
              (if (contains? &env doc)
                doc
                (or (resolve doc)
                    doc))
              doc)
        [attr-map children] (get-arg map? body)
        data (merged-data children &form doc attr-map)]
    `(let [suite# (binding [*context* (atom (suite {}))]
                    (swap! *context* merge ~data)
                    (run! #(if (sequential? %) (doall %) %) (flatten [~@children]))
                    @*context*)]
       (if *context*
         (swap! *context* update :suites conj suite#)
         suite#))))

(defmacro defdescribe
  [test-name & body]
  (let [[doc body] (get-arg string? body)
        [attr-map body] (get-arg map? body)
        test-var (list 'var (symbol (str *ns*) (str test-name)))
        attr-map (-> (meta test-name) 
                     (dissoc :doc)
                     (merge attr-map)
                     (assoc :var test-var))
        body (cond-> [(or doc (str test-name))]
               attr-map (conj attr-map)
               body (concat body))]
    `(def ~test-name
       (assoc (describe ~@body) :type :lazytest/var))))

(defmacro it
  [doc & body]
  (let [doc (if (symbol? doc)
              (if (contains? &env doc)
                doc
                (or (resolve doc) doc))
              doc)
        [attr-map body] (get-arg map? body)
        metadata (merged-data body &form doc attr-map)]
    `(let [test-case# (test-case
                       (assoc ~metadata :body (fn it# [] (let [ret# (do ~@body)] ret#))))]
       (if *context*
         (swap! *context* update :tests conj test-case#)
         test-case#))))

(defmacro before
  "Returns a context whose teardown function evaluates body."
  [& body]
  `(let [before-fn# (fn before# [] (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :before] conj before-fn#)
           nil)
       {:before before-fn#})))

(defmacro before-each
  [& body]
  `(let [before-each-fn# (fn before-each# [] (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :before]-each conj before-each-fn#)
           nil)
       {:before-each before-each-fn#})))

(defmacro after-each
  [& body]
  `(let [after-each-fn# (fn after-each# [] (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :after]-each conj after-each-fn#)
           nil)
       {:after-each after-each-fn#})))

(defmacro after
  "Returns a context whose teardown method evaluates body."
  [& body]
  `(let [after-fn# (fn after# [] (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :after] conj after-fn#)
           nil)
       {:after after-fn#})))

(defmacro around
  "Builds a function for the `around` context.

  Usage:
  (describe some-func
    {:context [(around [f]
                 (binding [*foo* 100]
                   (f)))]}
    ...)"
  [param & body]
  (assert (and (vector? param)
               (= 1 (count param))
               (simple-symbol? (first param))) "Must be a vector of one symbol")
  `(let [around-fn# (fn around# ~param (let [ret# (do ~@body)] ret#))]
     (if *context*
       (do (swap! *context* update-in [:context :around] conj around-fn#)
           nil)
       {:around after-fn#})))

(defn set-ns-context!
  "Must be a sequence of context maps, presumably built with the appropriate macros."
  [context]
  (alter-meta! *ns* assoc :lazytest/context (ctx/merge-context context)))

(comment
  (defdescribe context-tests
    {:context [(before (prn :attr-map))]}
    (before (prn :free-floating))
    (it "works???" (expect (= 1 1))))

  context-tests
  )

;;; filter2.clj

(defn focus-fns
  "Returns map of {:include? include-fn :exclude? exclude-fn}."
  [config]
  (let [include? (when-let [include (seq (:include config))]
                   (apply some-fn include))
        exclude? (when-let [exclude (seq (:exclude config))]
                   (apply some-fn exclude))]
    {:include? include?
     :exclude? exclude?}))

(comment
  (let [config (->config {:include #{:focus}
                          :exclude #{}})
        {:keys [include? exclude?]} (focus-fns config)
        m {:a :b :focus true}]
    (and (include? m)
         (not (exclude? m))))
  )

(defn walk-tree [tree]
  ((requiring-resolve 'clojure.walk/postwalk)
   #(if (:doc %) (select-keys % [:doc :type :tests :suites]) %)
   tree))

(defn filter-tree
  [suite config]
  (let [{:keys [include? exclude?]} (focus-fns config)]
    (letfn [(gather-items [given]
              (let [ret (reduce
                         (fn [{:keys [any-focused items]} cur]
                           (let [m (:metadata cur)
                                 this-excluded? (when exclude?
                                                  (exclude? m))
                                 this-focused? (or (:focus m)
                                                   (when include? (include? m)))
                                 cur (if this-focused?
                                       (assoc-in cur [:metadata :focus] true)
                                       cur)]
                             {:any-focused (or any-focused this-focused?)
                              :items (if this-excluded?
                                       items
                                       (conj items cur))}))
                         {:any-focused false
                          :items []}
                         given)
                    any-focused (:any-focused ret)]
                (when-let [fs (not-empty (:items ret))]
                  {:any-focused any-focused
                   :items (if any-focused
                            (filterv #(-> % :metadata :focus) fs)
                            fs)})))
            (filter-suite [suite]
              (let [m (:metadata suite)
                    this-excluded? (when exclude?
                                     (exclude? m))
                    this-focused? (or (:focus m)
                                      (when include? (include? m)))
                    suite (if this-focused?
                            (assoc-in suite [:metadata :focus] true)
                            suite)]
                (when-not this-excluded?
                  (let [suite (if this-focused?
                                (-> suite
                                    (update :tests #(mapv (fn [tc] (assoc-in tc [:metadata :focus] true)) %))
                                    (update :suites #(mapv (fn [suite] (assoc-in suite [:metadata :focus] true)) %)))
                                suite)
                        {tests-focused? :any-focused
                         tests :items} (some->> (:tests suite)
                                                (gather-items))
                        {suites-focused? :any-focused
                         suites :items} (some->> (:suites suite)
                                                 (keep filter-suite)
                                                 (gather-items))]
                    (when (or (seq tests)
                              (seq suites))
                      (cond-> (-> suite
                                  (assoc :tests tests)
                                  (assoc :suites suites))
                        (or tests-focused? suites-focused?)
                        (assoc-in [:metadata :focus] true)))))))]
      (filter-suite suite))))

(comment

  (defdescribe example-test
    (describe "poop"
      (let [i 50]
        (describe "nested"
          {:focus true}
          (it "works"
            (expect (= i (+ 49 1)))))
        (describe "other"
          (it "doesn't work"
            (expect (= 1 2))))))
    (describe "balls"
      {:focus true}
      (it "double works")))

  (defdescribe example-2-test
    (describe "poop"
      (let [i 50]
        (describe "nested"
          (it "works"
            {:integration true}
            (expect (= i (+ 49 1)))))
        (describe "other"
          (it "doesn't work"
            (expect (= 1 2))))))
    (describe "balls"))

  (walk-tree
   (filter-tree example-2-test (->config {:include #{:integration}})))

  (walk-tree
   (filter-tree
   @(defdescribe example-3-test
      "top"
      (describe "yes" {:focus true
                       :competing true}
        (it "works" {:integration true}))
      (describe "no" (it "doesn't")))
   (->config {:include #{:competing}
              :exclude #{:integration}})))

  )

;;; find2.clj

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
        ;; (def example (describe ...))
        (suite? value)
        (set-var value this-var)
        ;; (defn example {:test (describe ...)})
        (suite? test-metadata)
        (set-var test-metadata this-var)
        ;; (defn example {:test (it ...)})
        (test-case? test-metadata)
        (let [new-test (describe this-var)]
          (set-var (update new-test :tests conj test-metadata) this-var))
        ;; (defn example {:test #(expect ...)})
        (fn? test-metadata)
        (set-var (describe this-var (it "`:test` metadata" (test-metadata))) this-var)))))

(comment
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
  has :lazytest-suite metadata, uses that value instead.

  Always returns nil for the clojure.core namespace, to avoid special
  Vars such as *1, *2, *3"
  [n]
  (when-not (= (the-ns 'clojure.core) n)
    (or (:lazytest-suite (meta n))
        (when-let [s (test-suites-for-ns n)]
          (-> (meta n)
              (assoc :suites s)
              (assoc :doc (str (ns-name n)))
              (assoc :type :lazytest/ns)
              (suite))))))

(comment
  (find-ns-suite *ns*))

(defn find-suite
  "Returns test suite containing suites for the given namespaces.
  If no names given, searches all namespaces."
  [& names]
  (let [names (or (seq names) (all-ns))
        nses (mapv the-ns names)]
    (suite {:type :lazytest/run
              :nses nses
              :suites (keep find-ns-suite nses)})))

(comment
  (:suites (find-suite)))

;;; suite2.clj

(defn suite-result
  "Creates a suite result map with keys :source and :children.

  source is the test sequence, with identifying metadata.

  children is a sequence of test results and/or suite results."
  [source children]
  (let [{:keys [line file] :as sm} source
        doc (or (:doc sm) (:ns-name sm) (:var sm))]
    (assoc sm
           :type :lazytest.suite/suite-result
           :line line :file file :doc doc
           :source source
           :children children)))

;;; runner2.clj

(defn propagate-eachs
  [parent child]
  (-> child
      (assoc-in [:context :before-each]
                (into (vec (-> parent :context :before-each))
                      (-> child :context :before-each)))
      (assoc-in [:context :after-each]
                (into (vec (-> child :context :after-each))
                      (-> parent :context :after-each)))))

(defn run-befores
  [obj]
  (doseq [before-fn (-> obj :context :before)
          :when (fn? before-fn)]
    (before-fn)))

(defn run-before-eachs
  [obj]
  (doseq [before-each-fn (-> obj :context :before-each)
          :when (fn? before-each-fn)]
    (before-each-fn)))

(defn run-after-eachs
  [obj]
  (doseq [after-each-fn (-> obj :context :after-each)
          :when (fn? after-each-fn)]
    (after-each-fn)))

(defn run-afters
  [obj]
  (doseq [after-fn (-> obj :context :after)
          :when (fn? after-fn)]
    (after-fn)))

(defn combine-arounds
  [obj]
  (when-let [arounds (-> obj :context :around seq)]
    (c.t/join-fixtures arounds)))

(defn dispatch [m _config] (:type m))

(defmulti run-tree {:arglists '([m config])} #'dispatch)
(defmethod run-tree :default [m _config]
  (throw (ex-info "Non-test given to run-suite." {:obj m})))
(defmethod run-tree nil [_m _config])

(defn ->suite-result [suite config source-type]
  (let [id (:doc suite)
        start (System/nanoTime)
        config (-> config
                   (update :lazytest.runner/depth #(if id (inc %) %))
                   (update :lazytest.runner/suite-history conj suite))
        results (vec (keep #(run-tree % config) (concat (:tests suite)
                                                        (:suites suite))))
        duration (double (- (System/nanoTime) start))]
    (-> (suite-result suite results)
        (assoc :lazytest.runner/source-type source-type)
        (assoc :lazytest.runner/duration duration))))

(defmethod run-tree :lazytest/run
  run-test--lazytest-run
  [suite config]
  (report config (assoc suite :type :begin-test-run))
  (let [results (->suite-result suite config :lazytest/run)]
    (report config (assoc suite :type :end-test-run :results results))
    results))

(defmethod run-tree :lazytest/ns
  run-test--lazytest-ns
  [suite config]
  (report config (assoc suite :type :begin-test-ns))
  (let [results (->suite-result suite config :lazytest/ns)]
    (report config (assoc suite :type :end-test-ns :results results))
    results))

(defmethod run-tree :lazytest/var
  run-test--lazytest-var
  [suite config]
  (report config (assoc suite :type :begin-test-var))
  (let [results (->suite-result suite config :lazytest/var)]
    (report config (assoc suite :type :end-test-var :results results))
    results))

(defmethod run-tree :lazytest/suite
  run-test--lazytest-suite
  [suite config]
  (report config (assoc suite :type :begin-test-suite))
  (let [results (->suite-result suite config :lazytest/suite)]
    (report config (assoc suite :type :end-test-suite :results results))
    results))

(defmethod run-tree :lazytest/test-case
  run-suite--lazytest-test-case
  [tc config]
  (let [start (System/nanoTime)]
    (report config (assoc tc :type :begin-test-case))
    (let [results (try-test-case (with-meta (:body tc) tc))
          duration (double (- (System/nanoTime) start))
          results (assoc results :lazytest.runner/duration duration)]
      (report config results)
      (report config (assoc tc :type :end-test-case :results results))
      results)))

(defn run-tests
  "Runs tests defined in the given namespaces."
  ([namespaces] (run-tests {:reporter nested} namespaces))
  ([config namespaces]
   (let [suite (apply find-suite namespaces)
         suite (filter-tree suite config)]
     (run-tree suite config))))

(defn run-test-var
  [v config]
  (-> (find-var-test-value v)
      (assoc :type :lazytest/run)
      (filter-tree config)
      (run-tree config)))

(comment

  (defdescribe example-2-test
    (describe "poop"
      (let [i 50]
        (describe "nested"
          (it "works"
            {:integration true}
            (expect (= i (+ 49 1)))))
        (describe "other"
          (it "doesn't work"
            (expect (= 1 2))))))
    (describe "balls"))

  (let [config (->config {:reporter nested #_#_:include #{:integration}})]
    (run-test-var #'example-2-test config)
    nil)

  ;;
  ;; manually writing test cases (instead of using `it`)
  (defn common-test-cases [x]
    [(it (str x " equals one") (expect (= x 1)))
     (it (str x " equals two") (expect (= x 2)))])

  ;; using the above in a normal `defdescribe`
  (defdescribe s3 "Three"
    (map common-test-cases (range 2 4)))

  (run-tree (->config nil) s3))
