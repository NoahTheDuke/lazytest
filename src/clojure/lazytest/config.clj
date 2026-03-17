(ns lazytest.config
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [lazytest.hooks :refer [run-hooks]]))

(set! *warn-on-reflection* true)

(def version (delay (str/trim (slurp (io/resource "LAZYTEST_VERSION")))))
(defn lazytest-version [] (str "Lazytest v" @version))

;; inspired by kaocha.config/resolve-reporter
(defn resolve-reporter
  [reporter]
  (cond
    (sequential? reporter) (->> reporter
                                (map resolve-reporter)
                                (flatten)
                                (distinct)
                                (vec))
    (qualified-symbol? reporter)
    (if-let [r (requiring-resolve reporter)]
      (resolve-reporter (var-get r))
      (throw (ex-info (str "Cannot find reporter: " reporter)
                      {:reporter reporter})))
    (symbol? reporter) (resolve-reporter (symbol "lazytest.reporters" (name reporter)))
    :else reporter))

(defn resolve-hooks
  [hook]
  (cond
    (sequential? hook) (->> hook
                            (map resolve-hooks)
                            (flatten)
                            (distinct)
                            (vec))
    (qualified-symbol? hook)
    (if-let [h (requiring-resolve hook)]
      (resolve-hooks (var-get h))
      (throw (ex-info (str "Cannot find hook: " hook)
                      {:hook hook})))
    (symbol? hook) (resolve-hooks (symbol "lazytest.hooks" (name hook)))
    :else hook))

(defn to-seq [obj]
  (cond (sequential? obj) (vec (distinct obj))
        (nil? obj) []
        :else [obj]))

(defn ->config [config]
  (if (::generated config)
    config
    (let [output (to-seq (or (:output config) 'lazytest.reporters/nested))
          reporters (resolve-reporter output)
          hooks (to-seq (:hooks config))
          hooks (resolve-hooks hooks)
          config (-> config
                     (assoc ::generated true)
                     (assoc :lazytest.runner/depth 1)
                     (assoc :lazytest.runner/suite-history [])
                     (assoc :output output)
                     (assoc :reporters reporters)
                     (assoc :hooks hooks))]
      (run-hooks config config :config))))
