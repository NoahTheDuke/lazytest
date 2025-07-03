(ns lazytest.config
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def version (delay (str/trim (slurp (io/resource "LAZYTEST_VERSION")))))
(defn lazytest-version [] (str "Lazytest v" @version))

(defn combine-reporters
  ([reporter] (fn [config m] (reporter config m) (flush) nil))
  ([reporter & reporters]
   (fn [config m]
     (run! (fn [reporter] (reporter config m) (flush) nil)
           (cons reporter reporters)))))

;; inspired by kaocha.config/resolve-reporter
(defn resolve-reporter
  [reporter]
  (cond
    (sequential? reporter) (->> (flatten reporter)
                                (map resolve-reporter)
                                (distinct)
                                (apply combine-reporters))
    (qualified-symbol? reporter)
    (if-let [r (requiring-resolve reporter)]
      (resolve-reporter (var-get r))
      (throw (ex-info (str "Cannot find reporter: " reporter)
                      {:reporter reporter})))
    (symbol? reporter) (resolve-reporter (symbol "lazytest.reporters" (name reporter)))
    :else reporter))

(defn ->config [config]
  (if (:lazytest.runner/depth config)
    config
    (let [output (or (:output config) 'lazytest.reporters/nested)
          output (if (sequential? output) (distinct output) [output])
          reporter (resolve-reporter output)]
      (-> config
          (assoc :lazytest.runner/depth 1)
          (assoc :lazytest.runner/suite-history [])
          (assoc :output output)
          (assoc :reporter reporter)))))
