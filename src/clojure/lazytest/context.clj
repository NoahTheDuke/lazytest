(ns lazytest.context)

(defn combine-reporters
  ([reporter] (fn [context m] (reporter context m) (flush) nil))
  ([reporter & reporters]
   (fn [context m]
     (run! (fn [reporter] (reporter context m) (flush) nil)
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

(defn ->context [context]
  (let [runner (resolve-reporter
                 (or (:reporter context) 'lazytest.reporters/nested))
        runner (if (:verbose context)
                 (combine-reporters (resolve-reporter 'lazytest.reporters/verbose) runner)
                 runner)]
    (-> context
        (assoc :lazytest.runner/depth 1)
        (assoc :lazytest.runner/suite-history [])
        (assoc :reporter runner))))
