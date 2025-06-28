(ns lazytest.expectation-failed
  #?@(:bb []
      :clj [(:import
             (lazytest ExpectationFailed))]))

(set! *warn-on-reflection* true)

#_{:splint/disable [naming/lisp-case]}
(defn ->ExpectationFailed
  ([data] (->ExpectationFailed nil data))
  ([msg data]
   #?(:bb (ex-info (or msg "Expectation failed") (assoc data ::expectation-failed true))
      :clj (ExpectationFailed. msg data))))

(defn ex-failed?
  [^Throwable ex]
  #?(:bb (::expectation-failed (ex-data ex))
     :clj (instance? ExpectationFailed ex)))
