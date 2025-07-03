(ns lazytest.color 
  (:require
    [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def ^:dynamic *color*
  (contains? #{"yes" "true"} (System/getProperty "lazytest.colorize" "true")))

(defn colorize?
  "Colorize output, true if *color* is logical true."
  []
  *color*)

(def ^{:doc "ANSI color code table"} color-table
  {:reset "[0m"
   :black "[30m"
   :red "[31m"
   :green "[32m"
   :yellow "[33m"
   :blue "[34m"
   :magenta "[35m"
   :cyan "[36m"
   :white "[37m"
   :default "[39m"
   :bg-black "[40m"
   :bg-red "[41m"
   :bg-green "[42m"
   :bg-yellow "[43m"
   :bg-blue "[44m"
   :bg-magenta "[45m"
   :bg-cyan "[46m"
   :bg-white "[47m"
   :bg-default "[49m"
   :light "[90m"})

(defn ansi-color-str
  "Return ANSI color codes for the given sequence of colors, which are
  keywords in color-table."
  [& colors]
  (str/join (map (fn [c] (str (char 27) (color-table c))) colors)))

(defn colorize
  "Wrap string s in ANSI colors if colorize? is true."
  [s & colors]
  (if (and (colorize?) (seq s))
    (str (apply ansi-color-str colors) s (ansi-color-str :reset))
    s))
