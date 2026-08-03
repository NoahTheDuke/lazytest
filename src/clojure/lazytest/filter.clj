(ns lazytest.filter)

(set! *warn-on-reflection* true)

(defn focus-fns
  "Updates the provided config to have `:include-fn` and `:exclude-fn` based on `:include` and `:exclude` respectfully.
  When they are seqs, `:include` and `:exclude` sequences are applied to `some-fn`, returning a 1-arity function that checks if any of the include/exclude keywords exist on the arg."
  [config]
  (if (and (contains? config :include-fn)
        (contains? config :exclude-fn))
    config
    (let [include-fn (when-let [include (seq (:include config))]
                       (apply some-fn include))
          exclude-fn (when-let [exclude (seq (:exclude config))]
                       (apply some-fn exclude))]
      (-> config
        (assoc :include-fn include-fn)
        (assoc :exclude-fn exclude-fn)))))

(defn filter-tree-dispatch [obj _] (:type obj))

(defmulti filter-tree
  "If any item or sequence in the tree rooted at s has focus metadata
  set to true, returns just the focused items while preserving their
  position in the tree. Otherwise returns s unchanged."
  {:arglists '([obj config])}
  #'filter-tree-dispatch)

(defmethod filter-tree nil filter-tree--nil [_obj _config] nil)

(defn- gather-items [given config]
  (let [{:keys [include-fn exclude-fn] :as config} (focus-fns config)
        parent-focused (::parent-focused config)
        ret (reduce
              (fn [{:keys [any-focused items]} cur]
                (let [m (:metadata cur)
                      this-excluded? (or (:skip m)
                                       (when exclude-fn
                                         (exclude-fn m)))
                      this-focused? (or (:focus m)
                                      (when include-fn (include-fn m)))
                      cur (if this-focused?
                            (assoc-in cur [:metadata :focus] true)
                            cur)]
                  {:any-focused (or any-focused this-focused?)
                   :this-focused this-focused?
                   :items (cond
                            this-excluded? items
                            this-focused? (conj items cur)
                            (and include-fn (not parent-focused)) items
                            :else (conj items cur))}))
              {:any-focused false
               :this-focused false
               :items []}
              given)]
    (when-let [fs (not-empty (:items ret))]
      (assoc ret :items (if (:any-focused ret)
                          (filterv #(-> % :metadata :focus) fs)
                          fs)))))

(defn filter-suite
  "If any items in sequence s are focused, return them, with focus
  metadata added to the sequence; else return s unchanged."
  [suite config]
  (let [{:keys [include-fn] :as config} (focus-fns config)
        {focused? :any-focused
         children :items} (when-let [children (not-empty (:children suite))]
                            (let [m (:metadata suite)
                                  this-focused? (or (:focus m)
                                                  (when include-fn (include-fn m)))
                                  config (update config ::parent-focused #(or % this-focused?))]
                              (gather-items (->Eduction (keep #(filter-tree % config)) children) config)))]
    (when (seq children)
      (-> suite
        (assoc :children children)
        (cond-> focused? (assoc-in [:metadata :focus] true))))))

(comment
  (let [child1 {:type :lazytest/test-case}
        children [child1]]
    (filter-suite {:type :lazytest/suite
                   :metadata {:cool true}
                   :children children}
      {:include [:cool]})))

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
