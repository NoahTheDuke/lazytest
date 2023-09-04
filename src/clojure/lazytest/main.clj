(ns lazytest.main
  "Command-line test launcher."
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [lazytest.cli :refer [validate-opts]]
   [lazytest.report.console :as console]
   [lazytest.report.nested :as nested]
   [lazytest.report.summary :as summary]
   [lazytest.results :refer [summarize summary-exit-value]]
   [lazytest.runner :refer [run-tests]]
   [lazytest.tracker :refer [tracker]]))

(defn -main
  "Run with directories as arguments. Runs all tests in those
  directories; returns 0 if all tests pass."
  [& args]
  (let [{:keys [dir output
                exit-message ok]} (validate-opts args)]
    (when exit-message
      (println exit-message)
      (System/exit (if ok 0 1)))
    (let [namespaces ((tracker (map io/file dir) 0))
          _ (apply require namespaces)
          results (apply run-tests namespaces)
          summary (summarize results)]
      (case output
        "console" (console/report results)
        "nested" (nested/report results)
        nil)
      (summary/report results)
      (System/exit (summary-exit-value summary)))))
