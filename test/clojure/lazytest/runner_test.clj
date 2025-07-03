(ns lazytest.runner-test
  (:require
   [clojure.string :as str]
   [lazytest.config :refer [->config]]
   [lazytest.core :refer [*context* defdescribe describe expect it]]
   [lazytest.reporters :as reporters]
   [lazytest.runner :as sut]
   [lazytest.test-utils :refer [with-out-str-no-color]]))

(set! *warn-on-reflection* true)

(defn make-suite []
  (binding [*context* nil]
    (describe "here" (it "works" nil))))

(defdescribe run-test-suite-test
  (let [test-suite (make-suite)]
    (it "uses the var if given no doc string"
      (expect (= (str/join "\n"
                           ["  here"
                            "    √ works"
                            ""
                            ""])
                 (-> (sut/run-test-suite test-suite
                       (->config {:output reporters/nested*}))
                     (with-out-str-no-color)))))))

(defdescribe run-test-var-test
  (it "prints correctly"
    (expect
      (= (str/join "\n"
                   ["  run-test-suite-test"
                    "    √ uses the var if given no doc string"
                    ""
                    ""])
         (-> (sut/run-test-var #'run-test-suite-test
               (->config {:output reporters/nested*}))
             (with-out-str-no-color))))))
