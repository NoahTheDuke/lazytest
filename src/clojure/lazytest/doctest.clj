(ns lazytest.doctest
  (:require
   [clojure.string :as str]
   [cond-plus.core :refer [cond+]]) 
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
(require '[clojure.pprint :refer [pprint]])

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
      (throw (ex-info (str "Missing => result in code block") {:code code})))
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

(comment
  (parse-md example-file))

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

(def ^:dynamic *headers* (list))

(defn join-headers [sep]
  (str/join sep (map :title (reverse *headers*))))

(defn build-single-test
  [level [section & sections]]
  (cond
    (nil? section) nil
    (= :code (:type section))
    (-> (for [code (:code section)]
          (or (not-empty (:non code))
              (format
               "(defdescribe %s\n  (it %s\n     (expect ^{:line %s} (= %s %s))))"
               (gensym (str (slugify (join-headers "-")) "--"))
               (pr-str (join-headers " - "))
               (:line section)
               (:expected code)
               (:actual code))))
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
        tests (build-single-test 0 parsed-file)
        new-ns (slugify (str file))
        test-file (str (format "(ns %s)" new-ns)
                       "\n\n"
                       "(require '[lazytest.core :refer :all])"
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
  (lazytest.runner/run-tests [(the-ns 'readme-md)]))
