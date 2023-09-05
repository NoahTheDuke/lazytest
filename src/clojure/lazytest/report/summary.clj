(ns lazytest.report.summary
  (:require
   [clojure.data :refer [diff]]
   [clojure.pprint :as pp]
   [lazytest.color :refer [colorize]]
   [lazytest.results :refer [summarize]]
   [clojure.string :as str]
   [clojure.stacktrace :refer [print-cause-trace]]
   [lazytest.suite :refer [suite-result?]]))

(defn- identifier [result]
  (let [m (meta (:source result))]
    (or (:doc m) (:ns-name m))))

(defmacro pprint-out [obj]
  `(str/trim (with-out-str (pp/pprint ~obj))))

(defn- print-equality-failed [a b]
  (let [[a b same] (diff a b)]
    (println (colorize "Only in first argument:" :cyan))
    (pp/pprint a)
    (println (colorize "Only in second argument:" :cyan))
    (pp/pprint b)
    (when (some? same)
      (println (colorize "The same in both:" :cyan))
      (pp/pprint same))))

(defn- print-evaluated-arguments [reason]
  (println (colorize "Evaluated arguments:" :cyan))
  (doseq [arg (rest (:evaluated reason))]
    (print "* ")
    (pp/pprint arg)))

(defn- report-test-case-failure [result docs]
  (when-not (= :pass (:type result))
    (let [docs (conj docs (identifier result))
          report-type (:type result)
          docstring (format
                     "%s: %s"
                     (if (= :fail report-type) "FAILURE" "ERROR")
                     (str/join " " (remove nil? docs)))
          error (:thrown result)
          reason (ex-data error)]
      (println (colorize docstring :red))
      (printf "in %s:%s\n" (:file result) (:line result))
      (if (= :fail report-type)
        (do (println (colorize "Expression:" :cyan)
                     (pprint-out (:form reason)))
          (println (colorize "Result:" :cyan)
                   (pprint-out (:result reason)))
          (when (:evaluated reason)
            (print-evaluated-arguments reason)
            (when (and (= = (first (:evaluated reason)))
                       (= 3 (count (:evaluated reason))))
              (apply print-equality-failed (rest (:evaluated reason))))))
        (print-cause-trace error))
      (newline))))

(defn- report-failures [result docs]
  (if (suite-result? result)
    (doseq [child (:children result)]
      (report-failures child (conj docs (identifier result))))
    (report-test-case-failure result docs)))

(defn report [results]
  (let [summary (summarize results)
        {:keys [total not-passing]} summary
        count-msg (str "Ran " total " test cases.")]
    (println (if (zero? total)
               (colorize count-msg :yellow)
               count-msg))
    (println (colorize (str not-passing " failures.")
                       (if (zero? not-passing) :green :red)))
    (newline)
    (when (pos? not-passing)
      (report-failures results [])
      (flush))))
