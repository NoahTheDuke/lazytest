(ns lazytest.main
  "Command-line test launcher."
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.tools.namespace.file :refer [read-file-ns-decl]]
   [lazytest.cli :refer [validate-opts]]
   [lazytest.report.console :as console]
   [lazytest.report.nested :as nested]
   [lazytest.report.summary :as summary]
   [lazytest.results :refer [summarize summary-exit-value]]
   [lazytest.runner :refer [run-tests]]
   [lazytest.tracker :refer [find-sources]]))

(defn find-ns-decls [dirs]
  (mapv second (keep read-file-ns-decl (find-sources dirs))))

(defn require-dirs [dir]
  (let [dirs (map io/file (or dir #{"test"}))
        nses (find-ns-decls dirs)]
    (apply require nses)
    nses))

(defn run [{:keys [dir output]}]
  (let [nses (require-dirs dir)
        results (apply run-tests nses)]
    (case output
      "console" (console/report results)
      "nested" (nested/report results)
      nil)
    (summary/report results)
    (summarize results)))

(defn -main
  "Run with directories as arguments. Runs all tests in those
  directories; returns 0 if all tests pass."
  [& args]
  (let [{:keys [exit-message ok] :as opts} (validate-opts args)]
    (if exit-message
      (do (println exit-message)
        (System/exit (if ok 0 1)))
      (System/exit (summary-exit-value (run opts))))))
