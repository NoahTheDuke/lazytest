(ns lazytest.clojure.test
  (:require
   [clojure.test]
   [clojure.string :as str]
   [clojure.template :as temp]
   [lazytest.core :refer [defdescribe expect it]]))

(def ^:dynamic *testing-strs* (list))

(defmacro testing
  "Adapted from [clojure.test/testing]."
  [doc & body]
  `(binding [*testing-strs* (cons ~doc *testing-strs*)]
     ~@body))

(defn testing-str []
  (when (seq *testing-strs*)
    (str/join " " (reverse *testing-strs*))))

(defmacro is
  "Adapted from [clojure.test/is]."
  ([form] `(expect ~form (testing-str)))
  ([form msg]
   `(expect ~form (str (testing-str) "\n" ~msg))))

(defmacro are
  "Adapted from [clojure.test/are]."
  [argv expr & args]
  (if (or
       ;; (are [] true) is meaningless but ok
       (and (empty? argv) (empty? args))
       ;; Catch wrong number of args
       (and (pos? (count argv))
            (pos? (count args))
            (zero? (mod (count args) (count argv)))))
    (let [checks (map (fn [a] `(is ~(with-meta (temp/apply-template argv expr a) (meta &form)))) 
                      (partition (count argv) args))]
      `(do ~@checks))
    (throw (IllegalArgumentException. "The number of args doesn't match are's argv."))))

(defmacro deftest
  "Adapted from [clojure.test/deftest]."
  [test-name & body]
  (assert (symbol? test-name) "test-name must be a symbol")
  (with-meta
    `(defdescribe ~test-name
      (it "" ~@body))
    (meta &form)))
