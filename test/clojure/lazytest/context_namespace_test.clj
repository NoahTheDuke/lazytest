(ns lazytest.context-namespace-test
  (:require
   [lazytest.core :refer [around before-each defdescribe expect-it
                          set-ns-context!]]))

(set! *warn-on-reflection* true)

(def state (volatile! ::baseline))

(set-ns-context!
 [(around [f] (vreset! state []) (f) (vreset! state ::around-after))
  (before-each (vswap! state conj :before-each))])

(defdescribe context-namespace-test
  (expect-it "works"
    (= [:before-each] @state)))
