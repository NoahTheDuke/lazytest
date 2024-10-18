(ns lazytest.doctest 
  (:require
   [clojure.string :as str]
   [cond-plus.core :refer [cond+]]
   [medley.core :as med]) 
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
```

# Header 2
```clojure
(+ 1 1)
;; => 3
```

### Subheader 1
```clojure
(+ 2 2)
;;=> 4
```

## Subheader 2
```clojure
(+ 3 3)
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

(def output-marker-re #"(;+\s*)?=>\s*")

(defn parse-non-test-code [code]
  {:non code})

(defn parse-test-code [code]
  (let [[actual expected] (str/split code output-marker-re)]
    (when (nil? expected)
      (throw (ex-info (str "Missing ;;=> result in code block") {:code code})))
    {:expected (str/trim expected)
     :actual (str/trim actual)}))

(defn parse-code [code]
  (-> code
      (str/trim)
      (str/split #"\n\n")
      (->> (remove str/blank?)
           (map #(if (re-find output-marker-re %)
                   (parse-test-code %)
                   (parse-non-test-code %))))))

(defn parse-code-block
  [state ^Matcher m]
  (let [info-string (parse-info-string (.group m "infostring"))
        code (.group m "code")
        lang (.group m "lang")]
    (when-not (= "true" (get info-string "skip"))
      (when (#{"clojure" "clj"} lang)
        {:type :code
         :lang lang
         :code (parse-code code)
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

(defn build-single-test
  [lvl [section & sections]]
  (when section
    (let [[current sections]
          (if (= :header (:type section))
            (let [[children siblings] (split-with #(or (= :code (:type %))
                                                       (< lvl (:level %)))
                                        sections)
                  lvl' (:level (first (filter :level children)) lvl)
                  children-str (build-single-test lvl' children)]
              [(format "%s^{:line %s} (describe %s%s)"
                       (str/join (repeat (* 2 (dec lvl)) " "))
                       (:line section)
                       (pr-str (:title section))
                       (if children-str
                         (str "\n" children-str)
                         ""))
               siblings])
            (let [code-str (->> (:code section)
                                (filter :expected)
                                (map #(format "%s^{:line %s} (expect (= %s %s))"
                                              (str/join (repeat (* 3 lvl) " "))
                                              (:line section)
                                              (:expected %)
                                              (:actual %)))
                                (str/join "\n"))]
              [(if (str/blank? code-str)
                 ""
                 (format "%s^{:line %s} (it %s\n%s)"
                         (str/join (repeat (* 2 lvl) " "))
                         (:line section)
                         (pr-str (str "Line " (:line section)))
                         code-str))
               sections]))
          rest-of (build-single-test lvl sections)]
      (if rest-of
        (str current "\n\n" rest-of)
        current))))

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

(defn build-tests-for-file
  [[file file-str]]
  (let [parsed-file (parse-md file-str)
        non-test-code (->> {:children parsed-file}
                           (tree-seq (some-fn :children :code)
                                     (some-fn :children :code))
                           (keep :non))
        groups (med/partition-before #(= 1 (:level %)) parsed-file)
        tests (keep #(build-single-test (:level (ffirst groups) 1) %) groups)
        new-ns (slugify (str file))
        test-file (str (format "(ns %s)" new-ns)
                       "\n\n"
                       "(require '[lazytest.core :refer :all])"
                       "\n"
                       (str/join "\n\n" non-test-code)
                       "\n\n"
                       (->> tests
                            (map #(str/replace-first % "(describe " (format "(defdescribe %s " (gensym))))
                            (str/join "\n\n")))]
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
  (lazytest.runner/run-tests [(the-ns 'readme-md)]))
