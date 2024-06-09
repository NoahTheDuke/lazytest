(ns lazytest.report.nested
  "Nested doc printout:

  Namespaces
    lazytest.readme-test
      The square root of two
        <green>is less than two
        <green>is more than one
  ...

  "
  (:require
   [lazytest.color :refer [colorize]]
   [lazytest.suite :as s]
   [lazytest.test-case :as tc]))

(set! *warn-on-reflection* true)

(defn- identifier [result]
  (let [m (meta (:source result))]
    (or (:doc m) (:ns-name m))))

;;; Nested doc printout

(defn- indent [n]
  (print (apply str (repeat n "  "))))

(defn- dispatch [result] (:type (meta result)))

(defmulti nested
  {:arglists '([{:keys [source children depth] :as result}])}
  #'dispatch)

(defmethod nested ::s/suite-result
  [result]
  (let [id (identifier result)
        depth (:depth result)]
    (when id
      (indent depth)
      (println id))
    (let [depth (if id (inc depth) depth)]
      (doseq [child (:children result)
              :let [child (assoc child :depth depth)]]
        (nested child)))))

(defmethod nested ::tc/test-case-result
  [result]
  (when (identifier result)
    (indent (:depth result))
    (println (colorize (str (identifier result))
                       (if (= :pass (:type result)) :green :red)))))

;;; Entry point

(defn report [results]
  (nested (assoc results :depth 0))
  (flush)
  results)
