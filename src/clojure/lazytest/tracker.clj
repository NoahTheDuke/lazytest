(ns lazytest.tracker
  "Keeping track of which namespaces have changed and need to be reloaded"
  (:require
    [lazytest.dependency :refer [graph depend dependents remove-key]]
    [lazytest.nsdeps :refer [deps-from-ns-decl]]
    [clojure.tools.namespace.find :refer [find-sources-in-dir]]
    [clojure.tools.namespace.file :refer [read-file-ns-decl]]
    [clojure.set :refer [union]])
  (:import
    (java.io File)))

(set! *warn-on-reflection* true)

(defn find-sources
  [dirs]
  {:pre [(every? (fn [d] (instance? java.io.File d)) dirs)]}
  (mapcat find-sources-in-dir dirs))

(defn- newer-sources [dirs timestamp]
  (filter #(< timestamp (.lastModified ^File %)) (find-sources dirs)))

(defn- newer-namespace-decls [dirs timestamp]
  (remove nil? (map read-file-ns-decl (newer-sources dirs timestamp))))

(defn- add-to-dep-graph [dep-graph namespace-decls]
  (reduce (fn [g decl]
            (let [nn (second decl)
                  deps (deps-from-ns-decl decl)]
              (apply depend g nn deps)))
    dep-graph namespace-decls))

(defn- remove-from-dep-graph [dep-graph new-decls]
  (apply remove-key dep-graph (map second new-decls)))

(defn- update-dependency-graph [dep-graph new-decls]
  (-> dep-graph
    (remove-from-dep-graph new-decls)
    (add-to-dep-graph new-decls)))

(defn- affected-namespaces [changed-namespaces old-dependency-graph]
  (apply union (set changed-namespaces) (map #(dependents old-dependency-graph %)
                                          changed-namespaces)))

(defn tracker
  "Returns a no-arg function which, when called, returns a set of
  namespaces that need to be reloaded, based on file modification
  timestamps and the graph of namespace dependencies."
  [dirs initial-timestamp]
  {:pre [(integer? initial-timestamp)
         (every? (fn [f] (instance? java.io.File f)) dirs)]}
  (let [timestamp (atom initial-timestamp)
        dependency-graph (atom (graph))]
    (fn []
      (let [then @timestamp
            now (System/currentTimeMillis)
            new-decls (newer-namespace-decls dirs then)]
        (when (seq new-decls)
          (let [new-names (map second new-decls)
                affected-names (affected-namespaces new-names @dependency-graph)]
            (reset! timestamp now)
            (swap! dependency-graph update-dependency-graph new-decls)
            affected-names))))))
