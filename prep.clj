(ns prep
  (:require
   [clojure.string :as str]
   [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [opts]
  (println "Cleaning target")
  (b/delete {:path "target"})
  opts)

(defn javac [opts]
  (clean opts)
  (println "Compiling src/java")
  (b/javac {:src-dirs ["src/java"]
            :class-dir class-dir
            :basis @basis})
  (println "Compilation complete")
  opts)

(def lib 'io.github.noahtheduke/lazytest)
(def version (-> (slurp "./resources/LAZYTEST_VERSION")
                 (str/trim)
                 (str "-SNAPSHOT")))

(defn make-opts [opts]
  (assoc opts
    :lib lib
    :main 'lazytest.main
    :version version
    :basis @basis
    :scm {:tag (str "v" version)}
    :jar-file (format "target/%s-%s.jar" (name lib) version)
    :class-dir class-dir
    :src-dirs ["src/clojure"]
    :resource-dirs ["resources"]))

(defn install
  "Install built jar to local maven repo"
  [opts]
  (let [opts (make-opts opts)]
    (javac opts)
    (b/write-pom opts)
    (b/copy-dir {:src-dirs (concat (:src-dirs opts)
                                   (:resource-dirs opts))
                 :target-dir class-dir})
    (b/jar opts)
    (b/install opts)
    (println "Installed version" lib version)))
