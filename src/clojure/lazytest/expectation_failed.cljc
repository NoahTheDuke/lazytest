(ns lazytest.expectation-failed
  (:require
    [com.noahbogart.sinker :as sinker])
  #?@(:bb []
      :clj [(:import
             (lazytest ExpectationFailed))]))

(set! *warn-on-reflection* true)

#_{:splint/disable [naming/lisp-case]}
(defn ->ExpectationFailed
  ([data] (->ExpectationFailed nil data))
  ([msg data]
   (ex-info (or msg "Expectation failed")
            (assoc data ::sinker/type :lazytest/expectation-failed))))

(defn ex-failed?
  [^Throwable ex]
  (or (= :lazytest/expectation-failed (::sinker/type (ex-data ex)))
      #?(:bb false
         :clj (instance? ExpectationFailed ex))))
