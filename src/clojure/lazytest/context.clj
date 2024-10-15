(ns lazytest.context
  (:require
   [clojure.test :as c.t]))

(defn merge-context [context]
  (reduce
   (fn [acc cur]
     (reduce-kv
      (fn [m k v]
        (-> m
            (update k #(cond (vector? %) %
                             (some? %) [%]
                             :else []))
            (update k #(if (vector? v)
                         (into % v)
                         (conj % v)))))
      acc
      cur))
   {}
   context))

(comment
  (merge-context
   [{:before [1]}
    {:after 2}
    {:before 3}])) ; {:before [1 3], :after [2]}

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

(defn propagate-eachs
  [parent child]
  (-> child
      (assoc-in [:context :before-each]
                (into (-> parent :context :before-each vec)
                      (-> child :context :before-each)))
      (assoc-in [:context :after-each]
                (into (-> child :context :after-each vec)
                      (-> parent :context :after-each)))))

