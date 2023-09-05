(ns lazytest.results 
  (:require
    [lazytest.suite :refer [suite-result?]]))

(defn result-seq
  "Given a single suite result, returns a depth-first sequence of all
  nested child suite/test results."
  [result]
  (tree-seq suite-result? :children result))

(defn summarize
  "Given a sequence of suite results, returns a map of counts with
  keys :total, :pass, and :fail."
  [results]
  (let [test-case-results (remove suite-result? (result-seq results))
        total (count test-case-results)
        {:keys [pass fail error]} (group-by :type test-case-results)]
    {:total total
     :pass (count pass)
     :fail (count fail)
     :error (count error)
     :not-passing (+ (count fail) (count error))}))

(defn summary-exit-value
  "Given a summary map as returned by summarize, returns 0 if there
  are no failures and 1 if there are."
  [summary]
  (if (zero? (:not-passing summary)) 0 1))
