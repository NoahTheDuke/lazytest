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
      (throw (ex-info "Cannot find reporter" {:reporter reporter})))
    (symbol? reporter) (throw (ex-info "Cannot find reporter" {:reporter reporter}))
    (sequential? reporter) (->> reporter
                                (map resolve-reporter)
                                (apply combine-reporters))
    :else reporter))

(defn ->context [context]
  (-> context
      (assoc :lazytest.runner/depth 1)
      (assoc :lazytest.runner/suite-history [])
      (update :reporter
              #(if (fn? %) %
                 (resolve-reporter (or % 'lazytest.reporters/nested))))))

