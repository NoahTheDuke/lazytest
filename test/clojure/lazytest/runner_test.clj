(ns lazytest.runner-test 
  (:require
   [lazytest.config :refer [->config]]
   [lazytest.core :refer [*context* defdescribe describe expect it]]
   [lazytest.reporters :as reporters]
   [lazytest.runner :as sut]
   [lazytest.test-utils :refer [with-out-str-no-color]]
   [clojure.string :as str]))

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
                       (->config {:reporter reporters/nested*}))
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
               (->config {:reporter reporters/nested*}))
             (with-out-str-no-color))))))
