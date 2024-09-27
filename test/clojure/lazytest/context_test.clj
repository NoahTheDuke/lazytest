(ns lazytest.context-test
  (:require
   [lazytest.context :refer [propagate-eachs]]
   [lazytest.core :refer [after after-each around before before-each
                          defdescribe describe expect expect-it it]]))

(defn vconj! [volatile value]
  (vswap! volatile conj value))

(defdescribe broken-context-test
  (let [state (volatile! [])]
    (describe after
      {:context [(after (vconj! state :after))]}
      (expect-it "temp" (vconj! state :expect)))
    (describe "results"
      (expect-it "tracks correctly"
        (= [:expect :after] @state)))))

(comment
  (broken-context-test))

(defdescribe context-test
  {:focus true}
  (describe "on suites"
    (let [state (volatile! [])]
      (describe "manual maps"
        {:context [{:before (fn [] (vconj! state :before))
                    :after (fn [] (vconj! state :after))}]}
        (expect-it "temp" (vconj! state :expect)))
      (describe "results"
        (expect-it "tracks correctly"
          (= [:before :expect :after] @state))))
    (let [state (volatile! [])]
      (describe before
        {:context [(before (vconj! state :before))]}
        (expect-it "temp" (vconj! state :expect)))
      (describe "results"
        (expect-it "tracks correctly"
          (= [:before :expect] @state))))
    (let [state (volatile! [])]
      (describe after
        {:context [(after (vconj! state :after))]}
        (expect-it "temp" (vconj! state :expect)))
      (describe "results"
        (expect-it "tracks correctly"
          (= [:expect :after] @state))))
    (let [state (volatile! [])]
      (describe "not order dependent"
        {:context [(after (vconj! state :after))
                   (before (vconj! state :before))]}
        (expect-it "temp" (vconj! state :expect)))
      (describe "results"
        (expect-it "tracks correctly"
          (= [:before :expect :after] @state))))
    (let [state (volatile! [])]
      (describe "around"
        {:context [{:around (fn [f]
                              (vconj! state :around-before)
                              (f)
                              (vconj! state :around-after))}]}
        (expect-it "temp" (vconj! state :expect)))
      (describe "results"
        (expect-it "tracks correctly"
          (= [:around-before :expect :around-after] @state))))
    (let [state (volatile! [])]
      (describe around
        {:context [(around [f]
                           (vconj! state :around-before)
                           (f)
                           (vconj! state :around-after))]}
        (expect-it "temp" (vconj! state :expect)))
      (describe "results"
        (expect-it "tracks correctly"
          (= [:around-before :expect :around-after] @state))))
    (describe before-each
      (let [state (volatile! [])]
        (describe "inner"
          {:context [(before-each (vconj! state :before-each))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2))
          (expect-it "temp 3" (vconj! state :expect-3)))
        (describe "results"
          (expect-it "tracks correctly"
            (= [:before-each :expect-1 :before-each :expect-2 :before-each :expect-3] @state)))))
    (describe after-each
      (let [state (volatile! [])]
        (describe "inner"
          {:context [(after-each (vconj! state :after-each))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2))
          (expect-it "temp 3" (vconj! state :expect-3)))
        (describe "results"
          (expect-it "tracks correctly"
            (= [:expect-1 :after-each :expect-2 :after-each :expect-3 :after-each] @state)))))
    (describe "both before-each and after-each"
      (let [state (volatile! [])]
        (describe "inner"
          {:context [(before-each (vconj! state :before-each))
                     (after-each (vconj! state :after-each))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2)))
        (describe "results"
          (expect-it "tracks correctly"
            (= [:before-each :expect-1 :after-each :before-each :expect-2 :after-each] @state)))))
    (describe "complex flat case"
      (let [state (volatile! [])]
        (describe "inner"
          {:context [(before (vconj! state :before))
                     (before-each (vconj! state :before-each))
                     (after-each (vconj! state :after-each))
                     (after (vconj! state :after))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2)))
        (describe "results"
          (expect-it "tracks correctly"
            (= [:before :before-each :expect-1 :after-each :before-each :expect-2 :after-each :after] @state))))))
  (describe "on test cases"
    (let [state (volatile! [])]
      (it "works correctly"
        {:context [(before (vconj! state :before))
                   (after (vconj! state :after))]}
        (expect (vconj! state :expect)))
      (expect-it "tracks correctly"
        (= [:before :expect :after] @state)))
    (let [state (volatile! [])]
      (it "around"
        {:context [(around [f]
                     (vconj! state :before)
                     (f)
                     (vconj! state :after))]}
        (expect (vconj! state :expect)))
      (expect-it "tracks correctly"
        (= [:before :expect :after] @state)))))

(defdescribe propagate-eachs-test
  (expect-it "combines correctly"
    (= {:lazytest/context {:before-each [1 2 3 4 5 6]
                           :after-each []}}
       (meta (propagate-eachs {:lazytest/context {:before-each [1 2 3]}}
                              (with-meta [] {:lazytest/context {:before-each [4 5 6]}}))))))

(defdescribe complex-context-test
  (let [state (volatile! [])]
    (describe "top level"
      {:context [(before (vconj! state :before-top))
                 (before-each (vconj! state :before-each-top))
                 (after-each (vconj! state :after-each-top))
                 (after (vconj! state :after-top))]}
      (describe "middle level"
        {:context [(before (vconj! state :before-middle))
                   (before-each (vconj! state :before-each-middle))
                   (after-each (vconj! state :after-each-middle))
                   (after (vconj! state :after-middle))]}
        (describe "bottom level"
          {:context [(before (vconj! state :before-bottom))
                     (before-each (vconj! state :before-each-bottom))
                     (after-each (vconj! state :after-each-bottom))
                     (after (vconj! state :after-bottom))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2)))))
    (expect-it "tracks correctly"
      (= [:before-top
          :before-middle
          :before-bottom
          :before-each-top
          :before-each-middle
          :before-each-bottom
          :expect-1
          :after-each-bottom
          :after-each-middle
          :after-each-top
          :before-each-top
          :before-each-middle
          :before-each-bottom
          :expect-2
          :after-each-bottom
          :after-each-middle
          :after-each-top
          :after-bottom
          :after-middle
          :after-top] @state))))

(defdescribe multiple-same-eachs-test
  (let [state (volatile! [])]
    (describe "top level"
      {:context [(after-each (vconj! state :after-each-top))
                 (after-each (vconj! state :after-each-top-2))]}
      (describe "middle level"
        {:context [(after-each (vconj! state :after-each-middle))
                   (after-each (vconj! state :after-each-middle-2))]}
        (describe "bottom level"
          {:context [(after-each (vconj! state :after-each-bottom))
                     (after-each (vconj! state :after-each-bottom-2))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2)))))
    (expect-it "tracks correctly"
      (= [:expect-1
          :after-each-bottom
          :after-each-bottom-2
          :after-each-middle
          :after-each-middle-2
          :after-each-top
          :after-each-top-2
          :expect-2
          :after-each-bottom
          :after-each-bottom-2
          :after-each-middle
          :after-each-middle-2
          :after-each-top
          :after-each-top-2] @state))))
