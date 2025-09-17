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
  (let [files (concat
               (map io/file (:md config))
               (when (:doctests config)
                 (mapcat #(find-sources-in-dir % {:extensions [".md"]}) dirs)))]
    (->> files
         (map (juxt identity slurp))
         (keep dt/build-tests-for-file))))

(defn require-dirs [config dir]
  (let [dirs (map io/file (or dir #{"test"}))
        md-nses (add-md-tests config dirs)
        nses (into (find-ns-decls dirs)
                   md-nses)]
    (when (empty? nses)
      (throw (ex-info "No namespaces to run" {:dir dir})))
    (apply require nses)
    nses))

(defn run-impl [{:keys [dir] :as config}]
  (let [config (->config config)
        nses (require-dirs config dir)]
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
        (-> summary
            (assoc :results results)
            (assoc :exit (summary-exit-value summary)))))))

(defn -main
  "Pass-through to runner which does all the work."
  [& args]
  (let [{:keys [exit watch]} (run args)]
    (when-not watch
      (System/exit (or exit 0)))))
