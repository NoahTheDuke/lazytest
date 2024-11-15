(ns hooks.lazytest-core
  (:require [clj-kondo.hooks-api :as api]))

(defn get-arg
  "Taken from lazytest.core"
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn defdescribe
  "Converts (defdescribe foo ...body) to (def foo (fn [] ...body))"
  [{:keys [node] :as input}]
  (let [[defdescribe-node name-node & body-nodes] (:children node)
        [doc-node body-nodes] (get-arg api/string-node? body-nodes)
        doc-node (or doc-node
                     (with-meta (api/string-node (str name-node))
                       (meta name-node)))
        new-node (with-meta
                   (api/list-node
                    [(with-meta (api/token-node 'clojure.core/def)
                       (meta defdescribe-node))
                     name-node
                     doc-node
                     (api/list-node
                      (list*
                       (api/token-node 'clojure.core/fn)
                       (api/vector-node [])
                       body-nodes))])
                   (meta node))]
    (assoc input :node new-node)))
