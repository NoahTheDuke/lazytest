(ns lazytest.runner-test 
  (:require
   [lazytest.config :refer [->config]]
   [lazytest.core :refer [*context* defdescribe describe expect it]]
   [lazytest.reporters :as reporters]
   [lazytest.runner :as sut]
   [lazytest.test-utils :refer [with-out-str-no-color]]))

(defn make-suite []
  (binding [*context* nil]
    (describe "here" (it "works" nil))))

(defdescribe run-test-suite-test
  (let [test-suite (make-suite)]
    (it "uses the var if given no doc string"
      (expect (= "  here\n    √ works\n\n"
                 (-> (sut/run-test-suite test-suite
                       (->config {:reporter reporters/nested*}))
                     (with-out-str-no-color)))))))
