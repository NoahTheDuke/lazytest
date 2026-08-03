(ns step-definitions.friday-steps
  (:require
   [lazytest.core :refer [expect]]
   [lazytest.extensions.cucumber :refer [Given Then When]]))

(set! *warn-on-reflection* true)

(defn friday? [day]
  (if (= day "Friday")
    "TGIF"
    "Nope"))

(Given "today is {string}" [state given-day]
  (assoc state :today given-day))

(When "I ask whether it's Friday yet" [state]
  (assoc state :actual-answer (friday? (:today state))))

(Then "I should be told {string}" [state expected-answer]
  (expect (= expected-answer (:actual-answer state)))
  state)
