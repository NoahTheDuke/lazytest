(ns lazytest.clojure-ext.core
  (:import
   (java.util.regex Pattern)))

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
  ([s] (re-pattern s))
  ([s & flags]
   (assert (every? pattern-flags flags))
   (let [flags (map pattern-flags flags)]
     (if (= 1 (count flags))
       (Pattern/compile s (first flags))
       (Pattern/compile s (apply bit-and flags))))))

(comment
  (re-find (re-compile ".*#comment\n" :dotall :comments) "a\nb"))
