(ns lazytest.malli
  (:require
    [malli.core :as m]
    [malli.registry :as mr]))

(defonce ^:private registry* (atom {}))

(mr/set-default-registry!
  (mr/composite-registry
    ;; Built-in schemas
    (m/default-schemas)
    ;; Var registry
    (mr/var-registry)
    ;; Custom schemas
    (mr/mutable-registry registry*)))

(defmacro register!
  "Borrowed from malli documentation."
  [k ?schema]
  (assert (qualified-keyword? k) "Must provide a qualified keyword")
  `(swap! registry* assoc ~k ~?schema))

(register! :lt/file
           [:fn
            {:error/fn
             (fn [{:keys [value]} _x]
               (str "Expected a java.io.File, given " (type value)))}
            (fn [v] (instance? java.io.File v))])

(register! :lt/throwable
           [:fn
            {:error/fn
             (fn [{:keys [value]} _x]
               (str "Expected a throwable, given " (type value)))}
            (fn [v] (instance? Throwable v))])

(register! :lt/var
           [:fn
            {:error/fn
             (fn [{:keys [value]} _x]
               (str "Expected a Var, given " (type value)))}
            var?])
