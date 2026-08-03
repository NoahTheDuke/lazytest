(ns step-definitions.coffee-steps
  (:require
   [lazytest.core :refer [expect]]
   [lazytest.extensions.cucumber :refer [Given When And Then]]))

(set! *warn-on-reflection* true)

(Given "the following price list" [state table]
  (assoc state
    :price-list
    (into {}
      (map (fn [[k v]]
             [k (Double/parseDouble v)]))
      table)))

(When "I order a (.*)" [state product]
  (update state :order conj product))

(And "pay with ${double}" [{:keys [price-list order] :as state} paid]
  (doseq [product order]
    (expect (contains? price-list product)))

  (let [total (apply + (map price-list order))]
    (expect (<= total paid))

    (assoc state
           :total total
           :paid paid
           :change (- paid total))))

(Then "I get ${double} back" [{:keys [change] :as state} expected]
  (expect (= expected change))
  state)
