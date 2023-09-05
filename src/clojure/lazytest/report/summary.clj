(ns lazytest.report.summary
  (:require
   [clojure.data :refer [diff]]
   [clojure.pprint :as pp]
   [clojure.stacktrace :refer [print-cause-trace]]
   [clojure.string :as str]
   [lazytest.color :refer [colorize]]
   [lazytest.results :refer [summarize]]
   [lazytest.suite :as s]
   [lazytest.test-case :as tc]))

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

(defn- report-test-case-failure [result]
  (let [docs (conj (:docs result) (identifier result))
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
    (newline)))

(defn- dispatch [result]
  (:type (meta result)))

(defmulti summary
  {:arglists '([{:keys [source children docs] :as result}])}
  #'dispatch)

(defmethod summary ::s/suite-result
  [{:keys [docs children] :as results}]
  (doseq [child children
          :let [docs (conj docs (identifier results))
                child (assoc child :docs docs)]]
    (summary child)))

(defmethod summary ::tc/test-case-result
  [result]
  (when-not (= :pass (:type result))
    (report-test-case-failure result)))

(defn report [results]
  (let [{:keys [total not-passing]} (summarize results)
        count-msg (str "Ran " total " test cases.")]
    (println (if (zero? total)
               (colorize count-msg :yellow)
               count-msg))
    (println (colorize (str not-passing " failures.")
                       (if (zero? not-passing) :green :red)))
    (newline)
    (when (pos? not-passing)
      (summary (assoc results :docs []))
      (flush))))
