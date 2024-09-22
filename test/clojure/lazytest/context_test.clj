(ns lazytest.context-test 
  (:require
   [lazytest.core :refer [after before defdescribe describe expect-it given]]))

(defdescribe context-test
  (given [state (volatile! [])]
    (describe "manual maps"
      {:context [{:before (fn [] (vswap! state conj :before))
                  :after (fn [] (vswap! state conj :after))}]}
      (expect-it "temp" true))
    (expect-it "tracks correctly"
      (= [:before :after] @state)))
  (given [state (volatile! [])]
    (describe before
      {:context [(before (vswap! state conj :before))]}
      (expect-it "temp" true))
    (expect-it "tracks correctly"
      (= [:before] @state)))
  (given [state (volatile! [])]
    (describe after
      {:context [(after (vswap! state conj :after))]}
      (expect-it "temp" true))
    (expect-it "tracks correctly"
      (= [:after] @state)))
  (given [state (volatile! [])]
    (describe "not order dependent"
      {:context [(after (vswap! state conj :after))
                 (before (vswap! state conj :before))]}
      (expect-it "temp" true))
    (expect-it "tracks correctly"
      (= [:before :after] @state)))
  (given [state (volatile! [])]
    (describe "around"
      {:context [{:around (fn [f]
                            (vswap! state conj :around-before)
                            (f)
                            (vswap! state conj :around-after))}]}
      (expect-it "temp" true))
    (expect-it "tracks correctly"
      (= [:around-before :around-after] @state))))
