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

(defn filter-tree-dispatch [obj _] (:type obj))

(defmulti filter-tree
  "If any item or sequence in the tree rooted at s has focus metadata
  set to true, returns just the focused items while preserving their
  position in the tree. Otherwise returns s unchanged."
  {:arglists '([obj config])}
  #'filter-tree-dispatch)

(defmethod filter-tree nil [_obj _config] nil)

(defn filter-suite
  "If any items in sequence s are focused, return them, with focus
  metadata added to the sequence; else return s unchanged."
  [suite config]
  (let [{:keys [include? exclude?]} (focus-fns config)]
    (letfn [(gather-items [given]
             (let [ret (reduce
                         (fn [{:keys [any-focused items]} cur]
                           (let [m (:metadata cur)
                                 this-excluded? (or (:skip m)
                                                    (when exclude?
                                                      (exclude? m)))
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
                            fs)})))]
      (let [{focused? :any-focused
             children :items} (some->> (:children suite)
                                       (keep #(filter-tree % config))
                                       (gather-items))]
      (when (seq children)
        (-> suite
            (assoc :children children)
            (cond-> focused? (assoc-in [:metadata :focus] true))))))))

(defmethod filter-tree :lazytest/test-case
  filter-tree--lazytest-test-case
  [test-case _config]
  test-case)

(defmethod filter-tree :lazytest/suite
  filter-tree--lazytest-suite
  [suite config]
  (filter-suite suite config))

(defmethod filter-tree :lazytest/var
  filter-tree--lazytest-var
  [var-suite config]
  (let [var-filter (not-empty (:var-filter config))
        ns-filter (not-empty (:ns-filter config))]
    (if var-filter
      (when (or (var-filter (-> (:var var-suite) symbol))
                (when ns-filter
                  (ns-filter (-> (:var var-suite) symbol namespace symbol))))
        (filter-suite var-suite config))
      (filter-suite var-suite config))))

(defmethod filter-tree :lazytest/ns
  filter-tree--lazytest-ns
  [ns-suite config]
  (let [var-filter (map (comp symbol namespace) (:var-filter config))
        ns-filter (not-empty (set (concat var-filter (:ns-filter config))))]
    (if ns-filter
      (when (ns-filter (:doc ns-suite))
        (filter-suite ns-suite config))
      (filter-suite ns-suite config))))

(defmethod filter-tree :lazytest/run
  filter-tree--lazytest-run
  [run-suite config]
  (filter-suite run-suite config))
