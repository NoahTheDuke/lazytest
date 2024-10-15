(ns lazytest.filter-test
  (:require
   [lazytest.core :refer [defdescribe describe expect it before after]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.filter :as sut]
   [lazytest.suite :refer [suite test-var]]
   [lazytest.test-case :refer [test-case]]
   [lazytest.find :refer [find-ns-suite]]))

(def temp-var nil)

(defdescribe metadata-filtering-test
  (it "test-case"
    (let [tc (test-case {:body #(= 1 1)})]
      (expect (= tc (sut/filter-tree tc nil)))))
  (describe "suite"
    (it "doesn't modify inputs if nothing is filtered"
      (let [s (suite {:children [(test-case {:body #(= 1 1)})]})]
        (expect (= s (sut/filter-tree s nil)))))
    (it "filters to focused children"
      (let [tc1 (test-case {:doc "1"})
            tc2 (test-case {:doc "2"
                            :metadata {:focus true}})
            s (suite {:children [tc1 tc2]})]
        (expect (match? (assoc s
                               :children [tc2]
                               :metadata {:focus true})
                        (sut/filter-tree s nil)))))
    (it "filters to focused self"
      (let [tc1 (test-case {:doc "1"})
            tc2 (test-case {:doc "2"})
            s (suite {:children [tc1 tc2]
                      :metadata {:focus true}})]
        (expect (match? (assoc s :metadata {:focus true})
                        (sut/filter-tree s nil)))))
    (it "filters out skipped children"
      (let [tc1 (test-case {:doc "1"})
            tc2 (test-case {:doc "2"
                            :metadata {:skip true}})
            s (suite {:children [tc1 tc2]})]
        (expect (match? (assoc s :children [tc1])
                        (sut/filter-tree s nil)))))
    (it "prefers skip over focus"
      (let [tc1 (test-case {:doc "1"
                            :metadata {:focus true}})
            tc2 (test-case {:doc "2"
                            :metadata {:focus true
                                       :skip true}})
            s (suite {:doc "skip?"
                      :children [tc1 tc2]})]
        (expect (match? (assoc s
                               :metadata {:focus true}
                               :children [tc1])
                        (sut/filter-tree s nil))))))
  (describe "var"
    (it "works normally"
      (let [s (test-var {:var #'temp-var
                         :children
                         [(test-case {:body #(= 1 1)})]})]
        (expect (= s (sut/filter-tree s nil)))))
    (it "filters to focused children"
      (let [tc1 (test-case {:doc "1"})
            tc2 (test-case {:doc "2"
                            :metadata {:focus true}})
            s (test-var {:var #'temp-var
                         :children [tc1 tc2]})]
        (expect (= (assoc s
                          :children [tc2]
                          :metadata {:focus true})
                   (sut/filter-tree s nil)))))
    (it "filters to focused self"
      (let [tc1 (test-case {:doc "1"})
            tc2 (test-case {:doc "2"})
            s (test-var {:var #'temp-var
                         :children [tc1 tc2]
                         :metadata {:focus true}})]
        (expect (= (assoc s :metadata {:focus true})
                   (sut/filter-tree s nil))))))
  (describe "ns"
    {:context [(before (create-ns 'temp-ns))
               (after (remove-ns 'temp-ns))]}
    (it "works normally"
      (let [tc1 (test-var {:children [(test-case {:doc "1"})]})
            tc2 (test-var {:children [(test-case {:doc "2"})]})]
        (intern 'temp-ns 'tc1 tc1)
        (intern 'temp-ns 'tc2 tc2)
        (let [s (find-ns-suite (the-ns 'temp-ns))]
          (expect (match? s (sut/filter-tree s nil))))))
    (it "filters to focused children"
      (let [tc1 (test-var {:children [(test-case {:doc "1"})]})
            tc2 (test-var {:children [(test-case {:doc "2"})]
                           :metadata {:focus true}})]
        (intern 'temp-ns 'tc1 tc1)
        (intern 'temp-ns 'tc2 tc2)
        (let [s (find-ns-suite (the-ns 'temp-ns))]
          (expect (match?
                   (assoc s :children [(assoc tc2 :var var?)]
                          :metadata {:focus true})
                   (sut/filter-tree s nil))))))
    (it "filters to focused self"
      {:context [(before (alter-meta! (the-ns 'temp-ns) assoc :focus true))
                 (after (alter-meta! (the-ns 'temp-ns) dissoc :focus))]}
      (let [tc1 (test-var {:children [(test-case {:doc "1"})]})
            tc2 (test-var {:children [(test-case {:doc "2"})]})]
        (intern 'temp-ns 'tc1 tc1)
        (intern 'temp-ns 'tc2 tc2)
        (let [s (find-ns-suite (the-ns 'temp-ns))]
          (expect (match? (assoc s :metadata {:focus true})
                          (sut/filter-tree s nil)))))))
  )
