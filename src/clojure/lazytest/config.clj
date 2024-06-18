(ns lazytest.config)

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
    (qualified-symbol? reporter)
    (if-let [r (requiring-resolve reporter)]
      (resolve-reporter (var-get r))
      (throw (ex-info (str "Cannot find reporter: " reporter)
                      {:reporter reporter})))
    (symbol? reporter) (throw (ex-info (str "Cannot find reporter: " reporter)
                                       {:reporter reporter}))
    (sequential? reporter) (->> (flatten reporter)
                                (map resolve-reporter)
                                (apply combine-reporters))
    :else reporter))

(defn ->config [config]
  (let [runner (resolve-reporter
                 (or (:reporter config) 'lazytest.reporters/nested))]
    (-> config
        (assoc :lazytest.runner/depth 1)
        (assoc :lazytest.runner/suite-history [])
        (assoc :reporter runner))))
