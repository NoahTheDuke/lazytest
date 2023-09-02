(ns lazytest.main
  "Command-line test launcher."
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [lazytest.report.console :as console]
   [lazytest.report.summary :as summary]
   [lazytest.results :refer [summarize summary-exit-value]]
   [lazytest.runner :refer [run-tests]]
   [lazytest.tracker :refer [tracker]]))

(defn -main
  "Run with directories as arguments. Runs all tests in those
  directories; returns 0 if all tests pass."
  [& dirnames]
  (let [namespaces ((tracker (map io/file dirnames) 0))]
    (apply require namespaces)
    (let [results (apply run-tests namespaces)
          summary (summarize results)]
      (console/report results)
      (summary/report results)
      (System/exit (summary-exit-value summary)))))
