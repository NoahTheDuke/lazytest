(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]))

(def lib 'net.clojars.noahtheduke/lazytest)
(def version "0.1.0-SNAPSHOT")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))

(defn clean [opts]
  (println "Cleaning target")
  (b/delete {:path "target"})
  opts)

(defn compile-java [opts]
  (clean opts)
  (println "Compilng src/java")
  (b/javac {:src-dirs ["src/java"]
            :class-dir class-dir
            :basis basis
            :javac-opts ["--release" "11"]})
  (println "Compilation complete")
  opts)
