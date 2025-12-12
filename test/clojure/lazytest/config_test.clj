(ns lazytest.config-test
  (:require
   [lazytest.config :as sut]
   [lazytest.core :refer [defdescribe it expect]]
   [lazytest.extensions.matcher-combinators :refer [match?]]))

(set! *warn-on-reflection* true)

(defn example-reporter [& _args])

(defdescribe ->config-test
  (it "works"
    (expect
      (match? {:output [:keyword]
               :reporters vector?}
              (sut/->config {:output :keyword}))))
  (it "resolves by default to lazytest.reporters"
    (expect
      (match? {:output ['dots]
               :reporters vector?}
              (sut/->config {:output 'dots}))))
  (it "resolves other reporters correctly"
    (expect
      (match? {:output ['lazytest.config-test/example-reporter]
               :reporters vector?}
              (sut/->config {:output 'lazytest.config-test/example-reporter}))))
  (it "resolves multiple reporters"
    (expect
      (match? {:output ['dots 'lazytest.config-test/example-reporter]
               :reporters vector?}
              (sut/->config {:output ['dots 'lazytest.config-test/example-reporter]}))))
  (it "calls distinct on the input"
    (expect
      (match? {:output ['lazytest.config-test/example-reporter]
               :reporters vector?}
              (sut/->config {:output ['lazytest.config-test/example-reporter
                                      'lazytest.config-test/example-reporter]})))
    (expect
      (match? {:output ['lazytest.config-test/example-reporter 'dots]
               :reporters vector?}
              (sut/->config {:output ['lazytest.config-test/example-reporter
                                      'dots
                                      'lazytest.config-test/example-reporter]})))))
