(ns lazytest.main
  "Command-line test launcher."
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.tools.namespace.file :refer [read-file-ns-decl]]
   [clojure.tools.namespace.find :refer [find-sources-in-dir]]
   [lazytest.cli :refer [validate-opts]]
   [lazytest.malli]
   [lazytest.report.summary :refer [summary]]
   [lazytest.reporters :as reporters]
   [lazytest.results :refer [summarize summary-exit-value]]
   [lazytest.runner :refer [run-tests]]
   [malli.experimental :as mx]))

(mx/defn find-sources
  [dirs :- [:sequential :lt/file]]
  (mapcat find-sources-in-dir dirs))

(defn find-ns-decls [dirs]
  (mapv second (keep read-file-ns-decl (find-sources dirs))))

(defn require-dirs [dir]
  (let [dirs (map io/file (or dir #{"test"}))
        nses (find-ns-decls dirs)]
    (apply require nses)
    nses))

(defn resolve-reporter [output]
  (if-let [reporter-var (requiring-resolve (symbol "lazytest.reporters" output))]
    (let [reporter (var-get reporter-var)]
      (if (sequential? reporter)
        (apply reporters/combine-reporters reporter)
        (reporters/combine-reporters reporters/focused reporter)))
    (throw (ex-info (str "Can't find reporter: " output) {}))))

(defn run [{:keys [dir output]}]
  (let [nses (require-dirs dir)
        reporter (resolve-reporter output)
        results (run-tests {:reporter reporter} nses)]
    (summary results)
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
