(ns lazytest.scratch 
  (:require
   [criterium.bench :refer [bench]])
  (:import
    [java.util ArrayList]))

(set! *warn-on-reflection* true)

(def l (range 100))

(defn group-seq-by-1
  [pred coll]
  [(into [] (filter pred) coll)
   (into [] (remove pred) coll)])

(defn group-seq-by-2
  [pred coll]
  (->> coll
       (reduce
        (fn [acc cur]
          (if (pred cur)
            (update acc 0 conj cur)
            (update acc 1 conj cur)))
        [[] []])))

(defn group-seq-by-3
  [pred coll]
  (let [t (ArrayList/new)
        f (ArrayList/new)]
    (reduce
     (fn [_ cur]
       (if (pred cur)
         (.add t cur)
         (.add f cur)))
     nil
     coll)
    [(vec t)
     (vec f)]))

(comment
  (prn (= (group-seq-by-1 even? (doall (range 1000)))
          (group-seq-by-2 even? (doall (range 1000)))
          (group-seq-by-3 even? (doall (range 1000)))))
  (do
    (bench (group-seq-by-1 even? (doall (range 1000)))
           :limit-time-s 17.3)
    (flush)
    (bench (group-seq-by-2 even? (doall (range 1000)))
           :limit-time-s 17.3)
    (flush)
    (bench (group-seq-by-3 even? (doall (range 1000)))
           :limit-time-s 17.3)
    (flush))
  )
