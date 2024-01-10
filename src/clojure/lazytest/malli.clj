(ns lazytest.malli
  (:require
    [malli.core :as m]
    [malli.registry :as mr]))

(mr/set-default-registry!
  (mr/composite-registry
    ;; Built-in schemas
    (m/default-schemas)
    ;; Custom schemas
    {:lt/throwable [:fn
                    {:error/fn
                     (fn [{:keys [value]} _x]
                       (prn _x)
                       (str "Expected a throwable, given " (type value)))}
                    (fn [v] (instance? Throwable v))]}))
