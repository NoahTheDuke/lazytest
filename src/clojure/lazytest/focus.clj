(ns lazytest.focus)

(defn focus-fns
  "Returns map of {:include include-fn :exclude exclude-fn}."
  [config]
  (let [include? (when-let [include (:include config)]
                   (apply some-fn include))
        exclude? (when-let [exclude (:exclude config)]
                   (apply some-fn exclude))]
    {:include? include?
     :exclude? exclude?}))

(defn filter-focused
  "If any items in sequence s are focused, return them, with focus
  metadata added to the sequence; else return s unchanged."
  [{:keys [include? exclude?]} s]
  (let [ret (reduce
             (fn [{:keys [any-focused seqs]} cur]
               (let [m (meta cur)
                     this-excluded? (when exclude?
                                      (exclude? m))
                     this-focused? (or (:focus m)
                                       (when include? (include? m)))
                     cur (if this-focused?
                           (with-meta cur (assoc m :focus true))
                           cur)]
                 {:any-focused (or any-focused this-focused?)
                  :seqs (if this-excluded?
                          seqs
                          (conj seqs cur))}))
             {:any-focused false
              :seqs []}
             s)]
    (when-let [fs (seq (:seqs ret))]
      (if (:any-focused ret)
        (with-meta (filter (comp :focus meta) fs) (assoc (meta s) :focus true))
        (with-meta fs (meta s))))))

(defn filter-tree-impl
  [focus-fns s]
  (if (sequential? s)
    (let [m (meta s)
          {:keys [exclude?]} focus-fns]
      (when-not (when exclude? (exclude? m))
        (when-let [fs (not-empty (vec (keep #(filter-tree-impl focus-fns %) s)))]
          (filter-focused
           focus-fns
           (with-meta fs (meta s))))))
    s))

(defn filter-tree
  "If any item or sequence in the tree rooted at s has focus metadata
  set to true, returns just the focused items while preserving their
  position in the tree. Otherwise returns s unchanged."
  [config s]
  (let [focus-fn (focus-fns config)]
    (filter-tree-impl focus-fn s)))
