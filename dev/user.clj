(ns user
  (:require
    [clj-java-decompiler.core :as decompiler]
    [clojure.main :as main]
    [criterium.core :as criterium]))

(set! *warn-on-reflection* true)

; (def repl-requires
;   "A sequence of lib specs that are applied to `require`
;   by default when a new command-line REPL is started."
;   '[[clojure.repl :refer (source apropos dir pst doc find-doc)]
;     [clojure.java.javadoc :refer (javadoc)]
;     [clojure.pprint :refer (pp pprint)]])
(apply require main/repl-requires)

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
