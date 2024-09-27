(ns lazytest.filter)

(defn focus-fns
  "Returns map of {:include? include-fn :exclude? exclude-fn}."
  [config]
  (let [include? (when-let [include (seq (:include config))]
                   (apply some-fn include))
        exclude? (when-let [exclude (seq (:exclude config))]
                   (apply some-fn exclude))]
    {:include? include?
     :exclude? exclude?}))

#_(comment
  (let [config (->config {:include #{:focus}
                          :exclude #{}})
        {:keys [include? exclude?]} (focus-fns config)
        m {:a :b :focus true}]
    (and (include? m)
         (not (exclude? m))))
  )

(defn filter-tree
  "If any item or sequence in the tree rooted at s has focus metadata
  set to true, returns just the focused items while preserving their
  position in the tree. Otherwise returns s unchanged."
  [suite config]
  (let [{:keys [include? exclude?]} (focus-fns config)]
    (letfn [(gather-items [given]
              (let [ret (reduce
                         (fn [{:keys [any-focused items]} cur]
                           (let [m (:metadata cur)
                                 this-excluded? (when exclude?
                                                  (exclude? m))
                                 this-focused? (or (:focus m)
                                                   (when include? (include? m)))
                                 cur (if this-focused?
                                       (assoc-in cur [:metadata :focus] true)
                                       cur)]
                             {:any-focused (or any-focused this-focused?)
                              :items (if this-excluded?
                                       items
                                       (conj items cur))}))
                         {:any-focused false
                          :items []}
                         given)
                    any-focused (:any-focused ret)]
                (when-let [fs (not-empty (:items ret))]
                  {:any-focused any-focused
                   :items (if any-focused
                            (filterv #(-> % :metadata :focus) fs)
                            fs)})))
            (filter-suite [suite]
              (let [m (:metadata suite)
                    this-excluded? (when exclude?
                                     (exclude? m))
                    this-focused? (or (:focus m)
                                      (when include? (include? m)))
                    suite (if this-focused?
                            (assoc-in suite [:metadata :focus] true)
                            suite)]
                (when-not this-excluded?
                  (let [suite (if this-focused?
                                (-> suite
                                    (update :tests #(mapv (fn [tc] (assoc-in tc [:metadata :focus] true)) %))
                                    (update :suites #(mapv (fn [suite] (assoc-in suite [:metadata :focus] true)) %)))
                                suite)
                        {tests-focused? :any-focused
                         tests :items} (some->> (:tests suite)
                                                (gather-items))
                        {suites-focused? :any-focused
                         suites :items} (some->> (:suites suite)
                                                 (keep filter-suite)
                                                 (gather-items))]
                    (when (or (seq tests)
                              (seq suites))
                      (cond-> (-> suite
                                  (assoc :tests tests)
                                  (assoc :suites suites))
                        (or tests-focused? suites-focused?)
                        (assoc-in [:metadata :focus] true)))))))]
      (filter-suite suite))))

#_(comment

  (defn walk-tree [tree]
    ((requiring-resolve 'clojure.walk/postwalk)
     #(if (:doc %) (select-keys % [:doc :type :tests :suites]) %)
     tree))

  (defdescribe example-test
    (describe "poop"
      (let [i 50]
        (describe "nested"
          {:focus true}
          (it "works"
            (expect (= i (+ 49 1)))))
        (describe "other"
          (it "doesn't work"
            (expect (= 1 2))))))
    (describe "balls"
      {:focus true}
      (it "double works")))

  (defdescribe example-2-test
    (describe "poop"
      (let [i 50]
        (describe "nested"
          (it "works"
            {:integration true}
            (expect (= i (+ 49 1)))))
        (describe "other"
          (it "doesn't work"
            (expect (= 1 2))))))
    (describe "balls"))

  (walk-tree
   (filter-tree example-2-test (->config {:include #{:integration}})))

  (walk-tree
   (filter-tree
   @(defdescribe example-3-test
      "top"
      (describe "yes" {:focus true
                       :competing true}
        (it "works" {:integration true}))
      (describe "no" (it "doesn't")))
   (->config {:include #{:competing}
              :exclude #{:integration}})))
  )
