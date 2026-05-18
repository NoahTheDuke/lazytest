(ns lazytest.main
  "Command-line test launcher."
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.tools.namespace.file :refer [read-file-ns-decl]]
   [clojure.tools.namespace.find :refer [find-sources-in-dir]]
   [lazytest.cli :refer [validate-opts]]
   [lazytest.config :refer [->config]]
   [lazytest.doctest :as dt]
   [lazytest.results :refer [summarize summary-exit-value]]
   [lazytest.runner :refer [run-tests]]))

(defn find-ns-decls [dirs]
  (into []
        (comp (mapcat find-sources-in-dir)
              (keep read-file-ns-decl)
              (keep second))
        dirs))

(defn add-md-tests
  [config dirs]
  (let [files (into
               (mapv io/file (:md config))
               (when (:doctests config)
                 (->Eduction (mapcat #(find-sources-in-dir % {:extensions [".md"]})) dirs)))]
    (when (seq files)
      (->Eduction
       (comp
        (map (juxt identity slurp))
        (keep dt/build-tests-for-file))
       files))))

(defn require-dirs [config dirs]
  (let [dirs (mapv io/file (or dirs #{"test"}))
        md-nses (add-md-tests config dirs)
        nses (into (find-ns-decls dirs)
                   md-nses)
        ns-filter (not-empty (:ns-filter config))
        var-filter (not-empty (:var-filter config))
        nses (if (or ns-filter var-filter)
               (let [pred (into (set ns-filter) (map (comp symbol namespace)) var-filter)]
                 (filterv pred nses))
               nses)]
    (when (empty? nses)
      (throw (ex-info "No namespaces to run" {:dirs dirs})))
    (apply require nses)
    nses))

(defn run-impl [{:keys [dirs] :as config}]
  (let [config (->config config)
        nses (require-dirs config dirs)]
    (run-tests nses config)))

(defn run [args]
  (let [{:keys [exit-message ok] :as opts} (validate-opts args)]
    (cond
      exit-message
      (do (println exit-message)
          {:exit (if ok 0 1)})
      (:watch opts)
      (do (require 'lazytest.watch)
          (assoc opts :watcher ((resolve 'lazytest.watch/watch) run-impl opts)))
      :else
      (let [results (run-impl opts)
            summary (summarize results)]
        (flush)
        (-> summary
            (assoc :results results)
            (assoc :exit (summary-exit-value summary)))))))

(defn -main
  "Pass-through to runner which does all the work."
  [& args]
  (let [{:keys [exit watch]} (run args)]
    (when-not watch
      (System/exit (or exit 0)))))
