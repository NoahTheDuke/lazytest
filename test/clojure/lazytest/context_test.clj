(ns lazytest.context-test
  (:require
   [clojure.string :as str]
   [context-tests.use-fixture :refer [use-fixture-state]]
   [lazytest.context :refer [propagate-eachs]]
   [lazytest.core
    :refer [after after-each around around-each before before-each
            defdescribe describe expect expect-it it]]
   [lazytest.main :as main]
   [lazytest.runner :as lr]
   [lazytest.test-utils :refer [with-out-str-no-color]]))

(set! *warn-on-reflection* true)

(defn vconj! [volatile value]
  (vswap! volatile conj value))

(defdescribe broken-context-test
  (let [state (volatile! [])]
    (describe after
      {:context [(after (vconj! state :after))]}
      (expect-it "temp" (vconj! state :expect)))
    (expect-it "tracks correctly"
      (= [:expect :after] @state))))

(comment
  (broken-context-test))

(defdescribe context-test
  (describe "on suites"
    (let [state (volatile! [])]
      (describe "manual maps"
        {:context [{:before (fn [] (vconj! state :before))
                    :after (fn [] (vconj! state :after))}]}
        (expect-it "temp" (vconj! state :expect)))
      (expect-it "tracks correctly"
        (= [:before :expect :after] @state)))
    (let [state (volatile! [])]
      (describe before
        {:context [(before (vconj! state :before))]}
        (expect-it "temp" (vconj! state :expect)))
      (expect-it "tracks correctly"
        (= [:before :expect] @state)))
    (let [state (volatile! [])]
      (describe after
        {:context [(after (vconj! state :after))]}
        (expect-it "temp" (vconj! state :expect)))
      (expect-it "tracks correctly"
        (= [:expect :after] @state)))
    (let [state (volatile! [])]
      (describe "not order dependent"
        {:context [(after (vconj! state :after))
                   (before (vconj! state :before))]}
        (expect-it "temp" (vconj! state :expect)))
      (expect-it "tracks correctly"
        (= [:before :expect :after] @state)))
    (let [state (volatile! [])]
      (describe "around"
        {:context [{:around (fn [f]
                              (vconj! state :around-before)
                              (f)
                              (vconj! state :around-after))}]}
        (expect-it "temp" (vconj! state :expect)))
      (expect-it "tracks correctly"
        (= [:around-before :expect :around-after] @state)))
    (let [state (volatile! [])]
      (describe around
        {:context [(around [f]
                           (vconj! state :around-before)
                           (f)
                           (vconj! state :around-after))]}
        (expect-it "temp" (vconj! state :expect)))
      (expect-it "tracks correctly"
        (= [:around-before :expect :around-after] @state)))
    (describe before-each
      (let [state (volatile! [])]
        (describe "inner"
          {:context [(before-each (vconj! state :before-each))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2))
          (expect-it "temp 3" (vconj! state :expect-3)))
        (expect-it "tracks correctly"
          (= [:before-each :expect-1 :before-each :expect-2 :before-each :expect-3] @state))))
    (describe around-each
      (let [state (volatile! [])]
        (describe "inner"
          {:context [(around-each [f]
                       (vconj! state :around-each-before)
                       (f)
                       (vconj! state :around-each-after))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2)))
        (expect-it "tracks correctly"
          (= [:around-each-before
              :expect-1
              :around-each-after
              :around-each-before
              :expect-2
              :around-each-after] @state))))
    (describe after-each
      (let [state (volatile! [])]
        (describe "inner"
          {:context [(after-each (vconj! state :after-each))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2))
          (expect-it "temp 3" (vconj! state :expect-3)))
        (expect-it "tracks correctly"
          (= [:expect-1 :after-each :expect-2 :after-each :expect-3 :after-each] @state))))
    (describe "both before-each and after-each"
      (let [state (volatile! [])]
        (describe "inner"
          {:context [(before-each (vconj! state :before-each))
                     (after-each (vconj! state :after-each))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2)))
        (expect-it "tracks correctly"
          (= [:before-each :expect-1 :after-each :before-each :expect-2 :after-each] @state))))
    (describe "complex flat case"
      (let [state (volatile! [])]
        (describe "inner"
          {:context [(before (vconj! state :before))
                     (before-each (vconj! state :before-each))
                     (after-each (vconj! state :after-each))
                     (after (vconj! state :after))]}
          (expect-it "temp 1" (vconj! state :expect-1))
          (expect-it "temp 2" (vconj! state :expect-2)))
        (expect-it "tracks correctly"
          (= [:before :before-each :expect-1 :after-each :before-each :expect-2 :after-each :after] @state)))))
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
    (= {:context {:before-each [1 2 3 4 5 6]
                  :around-each []
                  :after-each []}}
       (propagate-eachs {:context {:before-each [1 2 3]}}
                        {:context {:before-each [4 5 6]}}))))

(defn top-level-ctx [state]
  [{:before (fn before1 [] (vconj! state :before-top))}
   {:before-each (fn before-each1 [] (vconj! state :before-each-top))}
   {:around (fn around1 [f] (vconj! state :around-before-top) (f) (vconj! state :around-after-top))}
   {:around-each (fn around-each1 [f] (vconj! state :around-each-before-top) (f) (vconj! state :around-each-after-top))}
   {:after-each (fn after-each1 [] (vconj! state :after-each-top))}
   {:after (fn after1 [] (vconj! state :after-top))}])

(defn middle-level-ctx [state]
  [{:before (fn before2 [] (vconj! state :before-middle))}
   {:before-each (fn before-each2 [] (vconj! state :before-each-middle))}
   {:around (fn around2 [f] (vconj! state :around-before-middle) (f) (vconj! state :around-after-middle))}
   {:around-each (fn around-each2 [f] (vconj! state :around-each-before-middle) (f) (vconj! state :around-each-after-middle))}
   {:after-each (fn after-each2 [] (vconj! state :after-each-middle))}
   {:after (fn after2 [] (vconj! state :after-middle))}])

(defn bottom-level-ctx [state]
  [{:before (fn before3 [] (vconj! state :before-bottom))}
   {:before-each (fn before-each3 [] (vconj! state :before-each-bottom))}
   {:around (fn around3 [f] (vconj! state :around-before-bottom) (f) (vconj! state :around-after-bottom))}
   {:around-each (fn around-each3 [f] (vconj! state :around-each-before-bottom) (f) (vconj! state :around-each-after-bottom))}
   {:after-each (fn after-each3 [] (vconj! state :after-each-bottom))}
   {:after (fn after3 [] (vconj! state :after-bottom))}])

(defn test-1-ctx [state]
  [{:before (fn before4 [] (vconj! state :before-tc))}
   {:before-each (fn before-each4 [] (vconj! state :before-each-tc))}
   {:around (fn around4 [f] (vconj! state :around-before-tc) (f) (vconj! state :around-after-tc))}
   {:around-each (fn around-each4 [f] (vconj! state :around-each-before-tc) (f) (vconj! state :around-each-after-tc))}
   {:after-each (fn after-each4 [] (vconj! state :after-each-tc))}
   {:after (fn after4 [] (vconj! state :after-tc))}])

(defdescribe complex-context-test
  (let [state (volatile! [])]
    (describe "top level"
      {:context (top-level-ctx state)}
      (describe "middle level"
        {:context (middle-level-ctx state)}
        (describe "bottom level"
          {:context (bottom-level-ctx state)}
          (expect-it "temp 1"
            {:context (test-1-ctx state)}
            (vconj! state :expect-1))
          (it "temp 2" (expect (vconj! state :expect-2))))))
    (expect-it "tracks correctly"
      (= [:before-top
          :around-before-top
          :before-middle
          :around-before-middle
          :before-bottom
          :around-before-bottom
          :before-tc
          :around-before-tc
          :around-each-before-top
          :around-each-before-middle
          :around-each-before-bottom
          :around-each-before-tc
          :before-each-top
          :before-each-middle
          :before-each-bottom
          :before-each-tc
          :expect-1
          :after-each-tc
          :after-each-bottom
          :after-each-middle
          :after-each-top
          :around-each-after-tc
          :around-each-after-bottom
          :around-each-after-middle
          :around-each-after-top
          :around-after-tc
          :after-tc
          :around-each-before-top
          :around-each-before-middle
          :around-each-before-bottom
          :before-each-top
          :before-each-middle
          :before-each-bottom
          :expect-2
          :after-each-bottom
          :after-each-middle
          :after-each-top
          :around-each-after-bottom
          :around-each-after-middle
          :around-each-after-top
          :around-after-bottom
          :after-bottom
          :around-after-middle
          :after-middle
          :around-after-top
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

(defdescribe set-ns-context-test
  (it "works like clojure.test/use-fixtures"
    (vreset! use-fixture-state [])
    (main/run ["--output" "quiet"
               "--dir" "corpus/context_tests"])
    (expect (= [:around-before :around-after] @use-fixture-state))))

(def ^:dynamic *issue-24-one* nil)
(def ^:dynamic *issue-24-two* nil)

(defn issue-24-suite []
  (describe "issue 24"
    {:context [(around [t]
                 (println "around 1 before")
                 (binding [*issue-24-one* 1] (t))
                 (println "around 1 after"))]}
    (println "in issue-24-suite")
    (it "test 1"
      (println "in test 1" *issue-24-one* *issue-24-two* "*one* *two*")
      (expect true))
    (describe "nested 1"
      {:context [(around [t]
                   (println "around 2 before")
                         (binding [*issue-24-two* 2] (t))
                         (println "around 2 after"))]}
      (println "in nested")
      (it "test 2"
        (println "in test 2" *issue-24-one* "*one*")
        (expect true))
      (it "test 3"
        (println "in test 3" *issue-24-two* "*two*")
        (expect true)))
    (it "test 4"
      (println "in test 4" *issue-24-one* *issue-24-two* "*one* *two*")
      (expect true))))

(defdescribe issue-24-test
  "Issue 24 asks about order of evaluation in describe blocks vs around calls"
  (expect-it "demonstrates the issue"
    (= "in issue-24-suite
in nested
  issue 24
around 1 before
in test 1 1 nil *one* *two*
    √ test 1
    nested 1
around 2 before
in test 2 1 *one*
      √ test 2
in test 3 2 *two*
      √ test 3
around 2 after
in test 4 1 nil *one* *two*
    √ test 4
around 1 after"
       (->> (lr/run-test-suite (issue-24-suite)
              {:output ['lazytest.reporters/nested*]})
            (with-out-str-no-color)
            (str/trim)))))

(defn root-ctx [state]
  [(before (vconj! state [:root :before]))
   (before-each (vconj! state [:root :before-each]))
   (around [f] (vconj! state [:root :pre-around]) (f) (vconj! state [:root :post-around]))
   (after (vconj! state [:root :after]))
   (after-each (vconj! state [:root :after-each]))])

(defn nested-ctx [state]
  [(before (vconj! state [:nest :before]))
   (before-each (vconj! state [:nest :before-each]))
   (around [f] (vconj! state [:nest :pre-around]) (f) (vconj! state [:nest :post-around]))
   (after (vconj! state [:nest :after]))
   (after-each (vconj! state [:nest :after-each]))])

(defn issue-31-suite [state]
  (describe "test-hook-ordering"
    {:context (root-ctx state)}
    (it "a" (vconj! state :in-first-test))
    (describe "nested"
      {:context (nested-ctx state)}
      (it "b" (vconj! state :in-second-test)))))

(defdescribe issue-31-test
  "Issue 31 asks about order of evaluation of hooks, especially when comparing suites and test cases"
  (let [state (volatile! [])]
    (it "demonstrates the issue"
      (->> (lr/run-test-suite (issue-31-suite state)
             {:output ['lazytest.reporters/nested*]})
           (with-out-str-no-color)
           (str/trim))
      (expect (= [[:root :before]
                  [:root :pre-around]
                  [:root :before-each]
                  :in-first-test
                  [:root :after-each]
                  [:nest :before]
                  [:nest :pre-around]
                  [:root :before-each]
                  [:nest :before-each]
                  :in-second-test
                  [:nest :after-each]
                  [:root :after-each]
                  [:nest :post-around]
                  [:nest :after]
                  [:root :post-around]
                  [:root :after]]
                 @state)))))
