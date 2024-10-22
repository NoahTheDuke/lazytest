(ns lazytest.doctest
  (:require
   [clojure.string :as str]
   [cond-plus.core :refer [cond+]]
   [lazytest.clojure-ext.core :refer [re-compile]]
   [medley.core :as med]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]) 
  (:import
   [java.util.regex Matcher]))

(set! *warn-on-reflection* true)

(def example-file
  "# Header 1

Lots of interesting stuff here

```clojure
(require '[clojure.string :as str])

(str/blank? \"\")
;; => true
(str/blank? \"a\")
;; => boolean?
```

# Header 2
```clojure
(+ 1 1)
;; => 2
(+ 1 2)
;; => 3
```

Watch out for side-effecting actions:
```clojure lazytest/skip=true
(System/exit 1)
```

### Subheader 1
```clojure
(require '[clojure.pprint :refer [pprint]])

(+ 2 2)
;;=> 4
```

## Subheader 2
```clojure
(str (+ 3 3))
;; =>6
```
")

(def header-re
  #"^(\s*\n)*(?<level>#+)\s+(?<title>.+)\s*\n")

(def code-re
  #"^```(?<lang>\w+)(?<infostring>( +\S+)*)\s*\n(?<code>(.|\n)*?)\n?```\s*\n")

(defn parse-info-string [info-string-str]
  (let [pairs (remove str/blank? (str/split info-string-str #"\s+"))]
    (reduce
     (fn [info-string pair]
       (let [[k v others] (str/split pair #"=")]
         (when (or (str/blank? k) (str/blank? v) others)
           (throw (ex-info (str "Invalid info-string entry: " pair)
                           {:info-string info-string})))
         (when (contains? info-string k)
           (throw (ex-info (str "Duplicate info-string entry: " pair)
                           {:info-string info-string})))
         (assoc info-string k v)))
     {}
     pairs)))

(def output-marker-re
  (re-compile "^(;+\\s*)?=>\\s*"
              :multiline))

(comment
  (str/split "(sql/format sqlmap)
=> ['SELECT a, b, c FROM foo WHERE foo.a = ?' 'baz']" output-marker-re))

(defn parse-code-block
  [state ^Matcher m]
  (let [info-string (parse-info-string (.group m "infostring"))
        code (.group m "code")
        lang (.group m "lang")]
    (when-not (= "true" (get info-string "lazytest/skip"))
      (when (#{"clojure" "clj"} (str/lower-case lang))
        {:type :code
         :lang lang
         :code code
         :info-string info-string
         :line (:line state)}))))

(defn parse-md
  [input]
  (loop [state {:blocks []
                :line 1}
         input input]
    (if (seq input)
      (let [[s l obj]
            (cond+ [(let [m (re-matcher header-re input)]
                      (when (.find m) m))
                    :> (fn [^Matcher m]
                         [(subs input (count (.group m)))
                          (get (frequencies (.group m)) \newline 0)
                          {:type :header
                           :level (count (.group m "level"))
                           :title (.group m "title")
                           :line (:line state)}])]
                   [(let [m (re-matcher code-re input)]
                      (when (.find m) m))
                    :> (fn [^Matcher m]
                         [(subs input (count (.group m)))
                          (get (frequencies (.group m)) \newline 0)
                          (parse-code-block state m)])]
                   [:else
                    (let [idx (str/index-of input "\n")]
                      (when-not (neg? idx)
                        [(subs input (inc idx)) 1]))])
            state (cond-> state
                    l (update :line + l)
                    obj (update :blocks conj obj))]
        (recur state s))
      (:blocks state))))

(comment
  (parse-md example-file))

(def ^:dynamic *headers* (list))

(defn join-headers [sep]
  (str/join sep (map :title (reverse *headers*))))

(defn slugify
  "As defined here: https://you.tools/slugify/"
  ([string] (slugify string "-"))
  ([string sep]
   (if-not (string? string) ""
     (as-> string $
       (java.text.Normalizer/normalize $ java.text.Normalizer$Form/NFD)
       (str/replace $ #"[^\x00-\x7F'`\"]+" "")
       (str/lower-case $)
       (str/trim $)
       (str/split $ #"[ \t\n\x0B\f\r!\"#$%&'()*+,-./:;<=>?@\\\[\]^_`{|}~]+")
       (filter seq $)
       (str/join sep $)))))

(defn assertion-zloc?
  [zloc]
  (and (= :token (z/tag zloc))
       (not (n/printable-only? (z/node zloc)))
       (= '=> (z/sexpr zloc))))

(defn make-test-node [line actual expected]
  (let [[a-row _col] (:position actual)
        [e-row _col] (:position expected)
        pos-diff (- e-row a-row)
        expected-sexpr (z/sexpr expected)]
    (n/list-node
     [(n/token-node 'lazytest.core/defdescribe)
      (n/spaces 1)
      (n/token-node (gensym (str (slugify (join-headers "-")) "--")))
      (if (zero? pos-diff) (n/spaces 1) (n/newlines 1))
      (n/list-node
       [(n/token-node 'lazytest.core/it)
        (n/spaces 1)
        (n/string-node (join-headers " - "))
        (cond
          (or (zero? pos-diff)
              (= 1 pos-diff)) (n/spaces 1)
          (< 1 pos-diff) (n/newlines 1))
        (n/list-node
         [(n/token-node 'lazytest.core/expect)
          (n/spaces 1)
          (n/meta-node
           (n/map-node [(n/keyword-node :line)
                        (n/spaces 1)
                        (n/token-node (+ a-row line))])
           (n/list-node
            (if (symbol? expected-sexpr)
              [(z/node expected)
               (n/spaces 1)
               (z/node actual)]
              [(n/token-node 'clojure.core/=)
               (n/spaces 1)
               (n/quote-node (z/node expected))
               (n/spaces 1)
               (z/node actual)])))])])])))

(defn rewrite-code
  [block]
  (let [code-str (-> (:code block)
                     (str/trim)
                     (str/replace output-marker-re "=> "))]
    (loop [zloc (z/of-string code-str {:track-position? true})]
      (let [zloc
            (cond
              (not zloc) (throw (ex-info "wtf" {}))
              (z/end? zloc) zloc
              (and (assertion-zloc? zloc)
                   (z/left zloc)
                   (z/right zloc))
              (let [actual (z/left zloc)
                    expected (z/right zloc)]
                (-> actual
                    (z/remove)
                    (z/next)
                    (z/remove)
                    (z/next)
                    (z/replace (make-test-node (:line block) actual expected))))
              :else zloc)]
        (if (z/end? zloc)
          (z/root-string zloc)
          (recur (z/next zloc)))))))

(comment
  (binding [*headers* [{:title "header 1"}]]
    (->> example-file
         (parse-md)
         (med/find-first #(= :code (:type %)))
         (rewrite-code)
         (println))))

(defn build-single-test
  [level [section & sections]]
  (cond
    (nil? section) nil
    (= :code (:type section))
    (-> [(rewrite-code section)]
        (concat (build-single-test level sections))
        (vec))
    (< level (:level section))
    (binding [*headers* (conj *headers* section)]
      (build-single-test (:level section) sections))
    :else
    (let [headers (drop-while #(<= (:level section) (:level %)) *headers*)]
      (binding [*headers* (conj headers section)]
        (build-single-test (:level section) sections)))))

(defn build-tests-for-file
  [[file file-str]]
  (let [parsed-file (parse-md file-str)
        parsed-file (if (= 1 (:level (first parsed-file)))
                      (next parsed-file)
                      parsed-file)
        tests (build-single-test 0 parsed-file)
        new-ns (slugify (str file))
        test-file (str (format "(ns %s\n  (:require [lazytest.core]))" new-ns)
                       "\n\n"
                       (str/join "\n\n" tests))]
    (try (Compiler/load (java.io.StringReader. test-file) (str file) (str file))
         (catch clojure.lang.Compiler$CompilerException ex
           (throw (ex-info (str "Failed to load doc test for " file)
                           {:file file
                            :test-file test-file}
                           ex))))
    (symbol new-ns)))

(comment
  (require '[lazytest.runner :as runner])
  (remove-ns 'readme-md)
  (build-tests-for-file ["README.md" example-file])
  (do (lazytest.runner/run-tests [(the-ns 'readme-md)])
      nil))
