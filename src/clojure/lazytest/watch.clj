(ns lazytest.watch
  (:require
   [clj-reload.core :as reload]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [lazytest.color :refer [colorize]])
  (:import
   [java.io File]
   [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(set! *warn-on-reflection* true)

(defn get-local-classpath []
  (-> (System/getProperty "java.class.path")
      (str/split (re-pattern File/pathSeparator))
      #_{:splint/disable [lint/into-literal]}
      (->> (into []
                 (comp (map io/file)
                       (remove #(File/.isAbsolute ^File %)))))))

(defn date->str [date]
  (let [fmt (java.text.SimpleDateFormat. "yyyy/MM/dd HH:mm:ss")]
    (.format fmt date)))

(defn reload-and-run [run-impl opts]
  (try
    (let [{:keys [loaded]} (reload/reload {:log-fn nil})]
      (when (seq loaded)
        (println "======================================================================")
        (newline)
        (println "Reloading" (str/join ", " loaded) "at" (date->str (java.util.Date.)))
        (newline)
        (run-impl opts)))
    (catch Throwable t
      (println "ERROR:" t)
      (.printStackTrace t)
      (newline)
      (println (colorize "ERROR while loading:" :red))
      (println t))))

(defn watch [run-impl opts]
  (reload/init
    {:dirs (get-local-classpath)
     :files #".*[.](clj|cljc|md)"})
  (let [opts (update opts :output #(or (not-empty %) ['lazytest.reporters/dots]))
        f (fn [] (reload-and-run run-impl opts))]
    ;; initial run
    (newline)
    (run-impl opts)
    (doto (ScheduledThreadPoolExecutor. 1)
      (.scheduleWithFixedDelay f 0 (:delay opts) TimeUnit/MILLISECONDS))))
