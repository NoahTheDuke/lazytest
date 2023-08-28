(ns lazytest.main
  "Command-line test launcher."
  (:gen-class)
  (:require
    [clojure.java.io :as io]
    [lazytest.runner.console :refer [run-tests]]
    [lazytest.tracker :refer [tracker]]
    [lazytest.report.nested :refer [report]]
    [lazytest.results :refer [summary-exit-value summarize]]))

(defn -main
  "Run with directories as arguments. Runs all tests in those
  directories; returns 0 if all tests pass."
  [& dirnames]
  (let [namespaces ((tracker (map io/file dirnames) 0))]
    (apply require namespaces)
    (let [results (apply run-tests namespaces)]
      (report results)
      (System/exit (summary-exit-value (summarize results))))))
