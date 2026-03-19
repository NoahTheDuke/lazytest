(ns lazytest.hooks
  "A hook is a multimethod that implements one or more of the hook methods.

  Hooks allow for modifying the state of a run while it is being executed."
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [clojure.string :as str]
   [lazytest.clojure-ext.core :refer [get-arg ->keyword]]
   [lazytest.clojure-ext.specs]
   [lazytest.results :refer [result-seq]]
   [lazytest.suite :as s]))

(set! *warn-on-reflection* true)

(defn run-hooks
  [config m hook-type]
  (let [hooks (:hooks config)]
    (if (and (sequential? hooks) (seq hooks))
      (let [m (assoc m ::hook-type hook-type)]
        (-> (reduce (fn [m f] (or (unreduced (f config m)) m)) m hooks)
            (dissoc ::hook-type)))
      m)))

(defn hook-dispatch [_config m] (::hook-type m))

(def all-hook-keys
  #{:cli-opts
    :config
    :pre-test-run
    :post-test-run
    :pre-test-suite
    :post-test-suite
    :pre-test-case
    :post-test-case})
(def ^:private all-hook-syms (into #{} (map symbol) all-hook-keys))

(defmacro defhook
  "`defhook` generates a conforming multimethod with correct dispatch and `:default` behavior. Provided methods (except `cli-opts`) must accept a config map and a relevant map (a suite or test-case generally), and must return an object of the same type.

  Allowed hook methods (with input notes):
  * cli-opts (input is a map with a `:new` vector)
  * config (input is the same `config` map)
  * pre-test-run
  * post-test-run
  * pre-test-suite
  * post-test-suite
  * pre-test-case
  * post-test-case

  Example usage with optional docstring:
  ```clojure
  (defhook yell
    \"Prints a message at the start and end of the whole run.\"
    (pre-test-run
      [config m]
      (println \"STARTING\")
      m)
    (post-test-run
      [config m]
      (println \"ENDING\")
      m)
    ...)
  ```"
  {:arglists '([hook-name & hook-methods] [hook-name docstring & hook-methods])}
  [hook-name & hook-impls]
  (let [[docstring hook-impls] (get-arg hook-impls string?)
        docstring (when docstring [docstring])]
    (assert (every? (comp symbol? first) hook-impls)
            "Must be given pairs of hook keyword to hook impls")
    (let [hook-keys (mapv (comp keyword first) hook-impls)
          method-namer (fn [hook-key] (symbol (str (name hook-name) "--" (name hook-key))))]
      (assert (empty? (set/difference (set hook-keys) all-hook-keys))
              "Only valid hook names, please")
      `(do (defmulti ~hook-name ~@docstring {:arglists '~'([config m])} (var ~`hook-dispatch))
           (defmethod ~hook-name :default ~(method-namer :default) ~'[_config m] ~'m)
           ~@(map (fn [[hook-key hook-args & hook-body]]
                    `(defmethod ~hook-name ~(keyword hook-key) ~(method-namer hook-key)
                       ~hook-args
                       ~@hook-body))
                  hook-impls)
           #'~hook-name))))

(spec/def-impl ::allowed-keys all-hook-syms #(contains? all-hook-syms %))
(spec/def ::body
  (spec/cat :params (spec/spec :lazytest.clojure-ext.specs/param-list)
            :body (spec/* any?)))
(spec/def ::impl
  (spec/cat :name ::allowed-keys
            :tail (spec/+ ::body)))
(spec/def ::defhook-args
  (spec/cat :defhook-name simple-symbol?
            :docstring (spec/? string?)
            :hooks (spec/+ (spec/spec ::impl))))
(spec/fdef defhook
  :args ::defhook-args
  :ret any?)

;; PROFILING
;; Print the top 5 namespaces and test vars by duration.
;; Code adapted from kaocha
;;
;; Example:
;;
;; blah blah blah

(defn start [m]
  (assoc m ::duration (System/nanoTime)))

(defn stop [m]
  (cond-> m
    (::duration m)
    (update ::duration #(double (- (System/nanoTime) %)))))

(defhook profiling
  "Print the slowest namespaces and test vars by duration."
  (cli-opts [_config opts]
    (update opts :new into
      [[nil "--[no-]profiling" "Print the slowest namespaces and test vars."
        :id :profiling/enabled
        :default true]
       [nil "--profiling-count COUNT" "Number of namespaces and test vars to print."
        :id :profiling/count
        :default 5
        :parse-fn #(Long/parseLong %)]]))
  (pre-test-suite
    [config m]
    (when (:profiling/enabled config)
      (start m)))
  (post-test-suite
    [config m]
    (when (:profiling/enabled config)
      (stop m)))
  (post-test-run
    [config results]
    (when (:profiling/enabled config)
      (let [cnt (or (:profiling/count config) 5)
            types (-> (group-by (comp :type :source) (result-seq results))
                    (select-keys [:lazytest/ns :lazytest/var])
                    (->> (reduce-kv (fn [m k v] (assoc m k (filterv ::duration v))) {})))
            total-duration (->> (mapcat identity (vals types))
                             (map ::duration)
                             (reduce + 0))
            slowest-ns-suites (take cnt (sort-by ::duration > (:lazytest/ns types)))
            ns-suite-duration (reduce + 0 (map ::duration slowest-ns-suites))
            slowest-vars (take cnt (sort-by ::duration > (:lazytest/var types)))
            var-duration (reduce + 0 (map ::duration slowest-vars))]
        (println (format "Top %s slowest test namespaces (%.5f seconds, %.1f%% of total time)"
                   (count slowest-ns-suites)
                   (double (/ ns-suite-duration 1e9))
                   (double (* (/ ns-suite-duration total-duration) 100))))
        (println (->> (for [suite slowest-ns-suites]
                        (format "  %s %.5f seconds"
                          (s/identifier suite)
                          (double (/ (::duration suite) 1e9))))
                   (str/join \newline)))
        (newline)
        (println (format "Top %s slowest test vars (%.5f seconds, %.1f%% of total time)"
                   (count slowest-vars)
                   (double (/ var-duration 1e9))
                   (double (* (/ var-duration total-duration) 100))))
        (println (->> (for [suite slowest-vars
                            :let [ns (some-> suite :source :var symbol namespace)
                                  id (str ns (when ns "/") (s/identifier suite))]]
                        (format "  %s %.5f seconds"
                          id
                          (double (/ (::duration suite) 1e9))))
                   (str/join \newline)))
        (flush)))))

;; RANDOMIZE
(defn shuffle-with-seed [^java.util.List coll ^java.util.Random rng]
  (let [al (java.util.ArrayList. coll)]
    (java.util.Collections/shuffle al rng)
    (vec al)))

(defhook randomize
  "Randomize the order of namespaces and suites in namespaces during a test run."
  (cli-opts [_config opts]
    (update opts :new into
      [[nil "--randomize TYPE" "Randomize the order of runs: all, ns, var, suite, or none."
        :id :randomize/type
        :default :all
        :default-desc "all"
        :parse-fn #(case (->keyword (str/lower-case %))
                     :all :all
                     (:ns :nss :nses) :ns
                     (:var :vars) :var
                     (:suite :suites) :suite
                     #_:else nil)]
       [nil "--randomize-seed NUM" "Seed for random shuffle."
        :id :randomize/seed
        :parse-fn #(Long/parseLong %)]]))
  (config [config _]
    (when (:randomize/type config)
      (as-> config $
        (update $ :randomize/seed #(or % (rand-int Integer/MAX_VALUE)))
        (assoc $ :randomize/rng (java.util.Random. (:randomize/seed $))))))
  (pre-test-suite [config suite]
    (let [random-level (:randomize/type config)
          suite-type (:type suite)]
      (when (and random-level
              (or (= :all random-level)
                (and (= :ns random-level) (= :lazytest/run suite-type))
                (and (= :var random-level) (= :lazytest/ns suite-type))
                (and (= :suite random-level) (#{:lazytest/var :lazytest/suite} suite-type))))
        (update suite :children shuffle-with-seed (:randomize/rng config)))))
  (post-test-run [config _run]
    (when (:randomize/type config)
      (printf "Ran with --seed %d\n" (:randomize/seed config)))))
