(ns lazytest.context 
  (:require
    [clojure.test :as c.t]))

(defn merge-context [contexts]
  (->> contexts
       (map #(update-vals % vector))
       (apply merge-with into)))

(comment
  (merge-context
   [{:before [1]}
    {:after 2}
    {:before 3}])) ; {:before [[1] 3], :after [2]}

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
  [parent-meta child]
  (let [child-meta (meta child)
        updated-meta (-> child-meta
                         (assoc-in [:context :before-each] (into (vec (-> parent-meta :context :before-each))
                                                                 (-> child-meta :context :before-each)))
                         (assoc-in [:context :after-each] (into (vec (-> child-meta :context :after-each))
                                                                (-> parent-meta :context :after-each))))]
    (with-meta child updated-meta)))
