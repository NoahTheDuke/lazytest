(ns lazytest.reporters-test
  (:require
   [lazytest.core :refer [defdescribe expect-it]]
   [lazytest.reporters :as sut]))

(defdescribe focused-test
  (expect-it "prints correctly"
    (= "=== FOCUSED TESTS ONLY ===\n\n"
       (with-out-str (sut/focused nil {:type :begin-test-run
                                       :focus true}))))
  (expect-it "prints nothing if not focused"
    (nil? (->> (sut/focused nil {:type :begin-test-run
                                 :focus false})
               (with-out-str)
               (not-empty)))))
