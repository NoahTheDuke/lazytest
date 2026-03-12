(ns lazytest.runner
  (:require
   [lazytest.config :refer [->config]]
   [lazytest.context :refer [combine-arounds combine-around-eachs
                             propagate-eachs run-after-eachs
                             run-afters run-before-eachs run-befores]]
   [lazytest.filter :refer [filter-tree]]
   [lazytest.find :refer [find-suite find-var-test-value]]
   [lazytest.hooks :as hooks]
   [lazytest.reporters :as r :refer [report]]
   [lazytest.suite :as s :refer [suite suite-result suite?]]
   [lazytest.test-case :refer [try-test-case]]))

(set! *warn-on-reflection* true)

(defn dispatch [m _config] (:type m))

(defmulti run-tree {:arglists '([m config])} #'dispatch)
(defmethod run-tree :default run-tree--default [m _config]
  (throw (ex-info "Non-test given to run-suite." {:obj m})))
(defmethod run-tree nil run-tree--nil [_m _config])

(defn ->suite-result [suite config source-type]
  (let [id (:doc suite)
        config (-> config
                   (update ::depth #(if id (inc %) %))
                   (update ::suite-history conj suite))
        around-fn (let [around-fn (or (combine-arounds suite)
                                      (fn [f] (f)))]
                    (fn with-around [f]
                      (let [ret (volatile! nil)]
                        (around-fn (fn [] (vreset! ret (f))))
                        @ret)))
        f #(let [child (propagate-eachs suite %)]
             (run-tree child config))
        results (around-fn #(vec (keep f (:children suite))))]
    (-> (suite-result suite results)
        (assoc ::source-type source-type))))

(defmethod run-tree :lazytest/run
  run-test--lazytest-run
  [suite config]
  (let [start (System/nanoTime)
        suite (hooks/run-hooks config suite :pre-test-run)]
    (report config (assoc suite :type :begin-test-run))
    (run-befores suite)
    (let [results (->suite-result suite config :lazytest/run)
          duration (double (- (System/nanoTime) start))
          results (assoc results ::duration duration)]
      (report config (-> suite
                         (assoc :type :end-test-run)
                         (assoc :results results)))
      (run-afters suite)
      (hooks/run-hooks config results :post-test-run))))

(defmethod run-tree :lazytest/ns
  run-test--lazytest-ns
  [suite config]
  (let [suite (hooks/run-hooks config suite :pre-test-suite)]
    (report config (assoc suite :type :begin-test-ns))
    (run-befores suite)
    (let [results (->suite-result suite config :lazytest/ns)]
      (report config (-> suite
                       (assoc :type :end-test-ns)
                       (assoc :results results)))
      (run-afters suite)
      (hooks/run-hooks config results :post-test-suite))))

(defmethod run-tree :lazytest/var
  run-test--lazytest-var
  [suite config]
  (let [suite (hooks/run-hooks config suite :pre-test-suite)]
    (report config (assoc suite :type :begin-test-var))
    (run-befores suite)
    (let [results (->suite-result suite config :lazytest/var)]
      (report config (-> suite
                       (assoc :type :end-test-var)
                       (assoc :results results)))
      (run-afters suite)
      (hooks/run-hooks config results :post-test-suite))))

(defmethod run-tree :lazytest/suite
  run-test--lazytest-suite
  [suite config]
  (let [suite (hooks/run-hooks config suite :pre-test-suite)]
    (report config (assoc suite :type :begin-test-suite))
    (run-befores suite)
    (let [results (->suite-result suite config :lazytest/suite)]
      (report config (-> suite
                       (assoc :type :end-test-suite)
                       (assoc :results results)))
      (run-afters suite)
      (hooks/run-hooks config results :post-test-suite))))

(defmethod run-tree :lazytest/test-case
  run-suite--lazytest-test-case
  [tc config]
  (let [tc (hooks/run-hooks config tc :pre-test-case)]
    (report config (assoc tc :type :begin-test-case))
    (run-befores tc)
    (let [results (let [around-fn (or (combine-arounds tc)
                                      (fn [f] (f)))
                        around-each-fn (or (combine-around-eachs tc)
                                       (fn [f] (f)))
                        ret (volatile! nil)]
                    (around-fn
                     (fn []
                      (around-each-fn
                       (fn []
                         (run-before-eachs tc)
                         (vreset! ret (try-test-case tc))
                         (run-after-eachs tc)))))
                    (assoc @ret ::source-type :lazytest/test-case))]
      (report config results)
      (report config (-> tc
                         (assoc :type :end-test-case)
                         (assoc :results results)))
      (run-afters tc)
      (hooks/run-hooks config results :post-test-case))))

(defn ^:no-doc filter-and-run
  [suite config]
  (let [config (->config config)]
    (-> suite
        (filter-tree config)
        (run-tree config))))

(defn run-tests
  "Runs tests defined in the given namespaces. Applies filters in config."
  ([namespaces] (run-tests namespaces (->config nil)))
  ([namespaces config]
   (-> (apply find-suite namespaces)
       (filter-and-run config))))

(defn run-all-tests
  "Run tests defined in all loaded namespaces. Applies filters in config."
  ([] (run-all-tests (->config nil)))
  ([config]
   (run-tests nil config)))

(defn run-test-var
  "Run test var. Looks for a suite as the var's value or in `:lazytest/test` metadata. Applies filters in config."
  ([v] (run-test-var v (->config nil)))
  ([v config]
   (assert (var? v) "Must be a var")
   (when-let [test-var (find-var-test-value v)]
     (-> (suite {:type :lazytest/run
                 :nses [(the-ns (symbol (namespace (symbol v))))]
                 :children [test-var]})
         (filter-and-run config)))))

(defn run-test-suite
  "Run test suite. Applies filters in config."
  ([s] (run-test-suite s (->config nil)))
  ([s config]
   (assert (suite? s) "Must provide a suite.")
   (-> (suite {:type :lazytest/run
               :children [s]})
       (filter-and-run config))))
