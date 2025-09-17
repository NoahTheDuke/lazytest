(ns lazytest.expectation-failed
  #?@(:bb []
      :clj [(:import
             (lazytest ExpectationFailed))]))

(set! *warn-on-reflection* true)

#_{:splint/disable [naming/lisp-case]}
(defn ->ExpectationFailed
  ([data] (->ExpectationFailed nil data))
  ([msg data]
   (ex-info (or msg "Expectation failed")
            (assoc data :type :lazytest/expectation-failed))))

(defn ex-failed?
  [^Throwable ex]
  (or (= :lazytest/expectation-failed (:type (ex-data ex)))
      #?(:bb false
         :clj (instance? ExpectationFailed ex))))
