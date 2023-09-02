(ns lazytest.report.summary 
  (:require
    [lazytest.color :refer [colorize]]
    [lazytest.results :refer [summarize]]))

(defn report [results]
  (let [summary (summarize results)
        {:keys [total not-passing]} summary
        count-msg (str "Ran " total " test cases.")]
    (println (if (zero? total)
               (colorize count-msg :yellow)
               count-msg))
    (println (colorize (str not-passing " failures.")
                       (if (zero? not-passing) :green :red)))))
