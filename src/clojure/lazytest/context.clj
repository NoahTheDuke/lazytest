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
  (doseq [before-fn (-> obj :lazytest/context :before)
          :when (fn? before-fn)]
    (before-fn)))

(defn run-before-eachs
  [obj]
  (doseq [before-each-fn (-> obj :lazytest/context :before-each)
          :when (fn? before-each-fn)]
    (before-each-fn)))

(defn run-after-eachs
  [obj]
  (doseq [after-each-fn (-> obj :lazytest/context :after-each)
          :when (fn? after-each-fn)]
    (after-each-fn)))

(defn run-afters
  [obj]
  (doseq [after-fn (-> obj :lazytest/context :after)
          :when (fn? after-fn)]
    (after-fn)))

(defn combine-arounds
  [obj]
  (when-let [arounds (-> obj :lazytest/context :around seq)]
    (c.t/join-fixtures arounds)))

(defn propagate-eachs
  [parent-meta child]
  (let [child-meta (meta child)
        updated-meta (-> child-meta
                         (assoc-in [:lazytest/context :before-each] (into (vec (-> parent-meta :lazytest/context :before-each))
                                                                 (-> child-meta :lazytest/context :before-each)))
                         (assoc-in [:lazytest/context :after-each] (into (vec (-> child-meta :lazytest/context :after-each))
                                                                (-> parent-meta :lazytest/context :after-each))))]
    (with-meta child updated-meta)))
