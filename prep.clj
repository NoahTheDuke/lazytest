(ns prep
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [opts]
  (println "Cleaning target")
  (b/delete {:path "target"})
  opts)

(defn javac [opts]
  (clean opts)
  (println "Compilng src/java")
  (b/javac {:src-dirs ["src/java"]
            :class-dir @class-dir
            :basis basis
            :javac-opts ["--release" "11"]})
  (println "Compilation complete")
  opts)
