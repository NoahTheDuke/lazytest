(ns lazytest.clojure-ext.core
  (:require
   [clojure.string :as str]) 
  (:import
   (java.util.regex Pattern)))

(set! *warn-on-reflection* true)

(def pattern-flags
  {:case-insensitive Pattern/CASE_INSENSITIVE
   :multiline Pattern/MULTILINE
   :dotall Pattern/DOTALL
   :unicode-case Pattern/UNICODE_CASE
   :canon-eq Pattern/CANON_EQ
   :unix-lines Pattern/UNIX_LINES
   :literal Pattern/LITERAL
   :unicode-character-class Pattern/UNICODE_CHARACTER_CLASS
   :comments Pattern/COMMENTS})

(defn re-compile
  "Returns an instance of java.util.regex.Pattern, for use, e.g. in `re-matcher`.

  If given only a string, acts as `re-pattern`. If given a string and trailing keywords, treats the keywords as java.util.regex.Pattern flags (cf. [[pattern-flags]]), using `bit-and` to merge them before passing them to `Pattern/compile`."
  (^Pattern [s] (re-pattern s))
  (^Pattern [s & flags]
   (assert (every? pattern-flags flags))
   (let [flags (map pattern-flags flags)]
     (if (= 1 (count flags))
       (Pattern/compile s (first flags))
       (Pattern/compile s (apply bit-and flags))))))

(comment
  (re-find (re-compile ".*#comment\n" :dotall :comments) "a\nb"))

(defn ^:no-doc get-arg
  "For internal use only.
  Pops first argument from args if (pred arg) is true.
  Returns a vector [first-arg remaining-args] or [nil args]."
  [args pred]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn ->keyword
  "Convert strings with optional leading colons to keywords.

  \"foo\" -> :foo
  \":foo\" -> :foo"
  [s]
  (keyword (if (str/starts-with? s ":") (subs s 1) s)))
