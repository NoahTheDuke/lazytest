(ns step-definitions.order
  (:require
   [lazytest.core :refer [expect]]
   [lazytest.extensions.cucumber :refer [Given Then When]]))

(Given "I have a (.*) fixture with value (.*)" [state fixture value]
  (assoc state fixture value))

(Given "there is a list" [state]
  (assoc state ::list []))

(When "I append {int} to the list" [state n]
  (update state ::list conj n))

(Then "(.*) should have value (.*)" [state fixture value]
  (expect (= (get state fixture) value))
  state)

(Then "the list should be {int} long" [state value]
  (expect (= value (count (::list state))))
  state)
