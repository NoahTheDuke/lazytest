(ns lazytest.core2-test
  (:require
   [clojure.math :as math]
   [lazytest.config :refer [->config]]
   [lazytest.core2 :as core2]
   [lazytest.main :as main]))

(defn run-impl [{:keys [dir output] :as config}]
  (let [output (or (not-empty output) ['lazytest.reporters/nested])
        config (->config (assoc config :output output :reporter output))
        nses (main/require-dirs config dir)]
    (core2/run-tests config nses)))

(core2/defdescribe square-root-test "The square root of two"
  (let [root (math/sqrt 2)]
    (core2/it "is less than two"
      (core2/expect (< root 2)))
    (core2/it "is more than one"
      (core2/expect (> root 1)))))
