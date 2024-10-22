(ns lazytest.dev
  (:require
   [clj-java-decompiler.core :as decompiler]
   [clojure.spec.test.alpha :as stest]
   [criterium.core :as criterium]))

(set! *warn-on-reflection* true)

(defmacro decompile
  "Decompile the form into Java and print it to stdout. Form shouldn't be quoted."
  [form]
  `(decompiler/decompile ~form))

(defmacro quick-bench
  "Convenience macro for benchmarking an expression, expr. Results are reported
  to *out* in human readable format. Options for report format are: :os,
  :runtime, and :verbose."
  [expr & opts]
  `(criterium/quick-bench ~expr ~@opts))

(defn spec-start! [] (stest/instrument))
(defn spec-stop! [] (stest/unstrument))

(spec-start!)
