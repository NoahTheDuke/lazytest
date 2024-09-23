(ns lazytest.context-namespace-test
  (:require
   [lazytest.core :refer [around before-each defdescribe expect-it
                          set-ns-context!]]))

(def state (volatile! nil))

(set-ns-context!
 [(around [f] (vreset! state []) (f) (vreset! state []))
  (before-each (vswap! state conj :before-each))])

(defdescribe context-namespace-test
  (expect-it "works"
    (= [:before-each] @state)))
