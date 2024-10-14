(ns lazytest.main
  "Command-line test launcher."
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.tools.namespace.file :refer [read-file-ns-decl]]
   [clojure.tools.namespace.find :refer [find-sources-in-dir]]
   [lazytest.cli :refer [validate-opts]]
   [lazytest.config :refer [->config]]
   [lazytest.malli]
   [lazytest.results :refer [summarize summary-exit-value]]
   [lazytest.runner :refer [run-tests]]
   [lazytest.watch :as watch]))

(defn find-ns-decls [config dirs]
  (let [var-filter-nses (->> (:var-filter config)
                             (map (comp symbol namespace))
                             (into #{}))
        ns-filter (or (not-empty (set/union (:ns-filter config) var-filter-nses))
                      any?)]
    (into []
          (comp (mapcat find-sources-in-dir)
                (keep read-file-ns-decl)
                (keep second)
                (filter ns-filter))
          dirs)))

(defn require-dirs [config dir]
  (let [dirs (map io/file (or dir #{"test"}))
        nses (find-ns-decls config dirs)]
    (when (empty? nses)
      (throw (ex-info "No namespaces to run" {})))
    (apply require nses)
    nses))

(defn run-impl [{:keys [dir output] :as config}]
  (let [output (or (not-empty output) ['lazytest.reporters/nested])
        config (->config (assoc config :output output :reporter output))
        nses (require-dirs config dir)]
    (run-tests nses config)))

(defn run [args]
  (let [{:keys [exit-message ok] :as opts} (validate-opts args)]
    (cond
      exit-message
      (do (println exit-message)
          {:exit (if ok 0 1)})
      (:watch opts)
      (assoc opts :watcher (watch/watch run-impl opts))
      :else
      (let [results (run-impl opts)
            summary (summarize results)]
        (assoc summary
               :results results
               :exit (summary-exit-value summary))))))

(defn -main
  "Pass-through to runner which does all the work."
  [& args]
  (let [{:keys [exit watch]} (run args)]
    (when-not watch
      (System/exit (or exit 0)))))
