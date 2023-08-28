(ns lazytest.watch
  (:gen-class)
  (:require
    [clojure.java.io :refer [file]]
    [clojure.string :refer [join]]
    [lazytest.color :refer [colorize]]
    [lazytest.reload :refer [reload]]
    [lazytest.report.nested :as nested]
    [lazytest.runner.console :as console]
    [lazytest.tracker :refer [tracker]])
  (:import
    (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)))

(defn reload-and-run [tracker run-fn report-fn]
  (try
    (let [new-names (seq (tracker))]
      (when new-names
        (println)
        (println "======================================================================")
        (println "At" (java.util.Date.))
        (println "Reloading" (join ", " new-names))
        (apply reload new-names)
        (report-fn (apply run-fn new-names))
        (println "\nDone.")))
    (catch Throwable t
      (println "ERROR:" t)
      (.printStackTrace t)
      (newline)
      (println (colorize "ERROR while loading:" :red))
      (println t))))

(defn reloading-runner [dirs run-fn report-fn]
  (let [dirs (map file dirs)
        track (tracker dirs 0)]
    (fn [] (reload-and-run track run-fn report-fn))))

(defn start [dirs & options]
  (let [{:keys [run-fn report-fn delay]
         :or {run-fn console/run-tests
              report-fn nested/report
              delay 500}} options
        f (reloading-runner dirs run-fn report-fn)]
    (doto (ScheduledThreadPoolExecutor. 1)
      (.scheduleWithFixedDelay f 0 delay TimeUnit/MILLISECONDS))))

(defn -main [& args]
  (start args))
