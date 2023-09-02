(ns lazytest.runner.debug
  (:require
   [clojure.stacktrace :refer [print-cause-trace]]
   [lazytest.find :refer [find-suite]]
   [lazytest.suite :refer [expand-tree suite suite-result suite? test-seq]]
   [lazytest.test-case :refer [test-case? try-test-case]])
  (:import
   (lazytest ExpectationFailed)))

(defn identifier [x]
  (let [m (meta x)]
    (str (or (:name m)
             (:doc m)
             (System/identityHashCode x))
      " (" (:file m) ":" (:line m) ")")))

(defn run-test-case [tc]
  (println "Running test case" (identifier tc))
  (let [result (try-test-case tc)]
    (prn result)
    (when-let [t (:thrown result)]
      (when (instance? ExpectationFailed t)
        (prn (ex-data t)))
      (print-cause-trace t 5))
    (println "Done with test case" (identifier tc))
    result))

(defn run-suite [ste]
  (let [ste-seq (ste)]
    (println "Running suite" (identifier ste-seq))
    (let [results (doall (map (fn [x]
                                (cond (suite? x) (run-suite x)
                                  (test-case? x) (run-test-case x)
                                  :else (throw (IllegalArgumentException.
                                                 "Non-test given to run-suite."))))
                           ste-seq))]
      (println "Done with suite" (identifier ste-seq))
      (suite-result ste-seq results))))

(defn run-tests
  "Runs tests defined in the given namespaces, with verbose output."
  [& namespaces]
  (run-suite (apply find-suite namespaces)))

(defn run-all-tests
  "Run tests defined in all namespaces with verbose output."
  []
  (run-tests))

(defn run-test-var
  [v]
  {:pre [(var? v)]}
  (let [tree (-> (suite @v)
                 (expand-tree)
                 (test-seq))]
    (run-suite tree)))
