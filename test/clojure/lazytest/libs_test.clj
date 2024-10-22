(ns lazytest.libs-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.gitlibs :as gl]
   [lazytest.config :refer [->config]]
   [lazytest.core :refer [before defdescribe expect it]]
   [lazytest.doctest :refer [build-tests-for-file slugify]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.results :refer [summarize]]
   [lazytest.runner :as lr]))

(defdescribe honeysql-test
  (let [honeysql (delay (gl/procure "https://github.com/seancorfield/honeysql.git" 'com.github.seancorfield/honeysql "v2.6.1196"))
        readme (delay (io/file @honeysql "README.md"))
        readme-str (delay (-> (slurp @readme)
                              (str/replace
                               #".!-- :test-doc-blocks/skip --.\n```clojure\n"
                               "```clojure lazytest/skip=true\n")
                              (str/replace
                               #".!-- \{:test-doc-blocks/reader-cond :cljs} --.\n```.lojure\n"
                               "```clojure lazytest/skip=true\n")))]
    (it "can run all readme tests"
      {:context [(before (remove-ns (symbol (slugify (str @readme)))))]}
      (let [honeysql-ns (let [s (java.io.StringWriter.)]
                          (binding [*err* s]
                            (build-tests-for-file [@readme @readme-str])))]
        (expect
          (match? {:total 80 :pass 80 :fail 0}
                  (summarize
                   (lr/run-tests [(the-ns honeysql-ns)]
                                 (->config {:reporter ['lazytest.reporters/quiet]})))))))))
