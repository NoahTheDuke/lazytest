(ns lazytest.malli
  (:require
    [malli.core :as m]
    [malli.registry :as mr]))

(mr/set-default-registry!
  (mr/composite-registry
    ;; Built-in schemas
    (m/default-schemas)
    ;; Custom schemas
    {:lt/file [:fn
               {:error/fn
                (fn [{:keys [value]} _x]
                  (str "Expected a java.io.File, given " (type value)))}
               (fn [v] (instance? java.io.File v))]
     :lt/throwable [:fn
                    {:error/fn
                     (fn [{:keys [value]} _x]
                       (str "Expected a throwable, given " (type value)))}
                    (fn [v] (instance? Throwable v))]
     :lt/var [:fn
              {:error/fn
               (fn [{:keys [value]} _x]
                 (str "Expected a Var, given " (type value)))}
              var?]}))
