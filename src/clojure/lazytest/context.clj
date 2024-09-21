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

(defn run-afters
  [obj]
  (doseq [after-fn (-> obj :context :after)
          :when (fn? after-fn)]
    (after-fn)))

(defn combine-arounds
  [obj]
  (when-let [arounds (-> obj :context :around seq)]
    (c.t/join-fixtures arounds)))
