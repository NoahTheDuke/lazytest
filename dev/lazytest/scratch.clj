(ns lazytest.scratch 
  {:clj-kondo/ignore [:unused-namespace :unused-referred-var]}
  (:require
   [criterium.bench :refer [bench]]
   [clj-async-profiler.core :as prof]))

(set! *warn-on-reflection* true)

(comment
  (prof/serve-ui 8080) ; Serve on port 8080
  )
