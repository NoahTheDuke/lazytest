(ns build
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.build.api :as b]
   [noahtheduke.clein.pom-data :as clein.pom]
   [deps-deploy.deps-deploy :as dd]))

(defn make-version []
  (-> (slurp "./resources/LAZYTEST_VERSION")
      (str/trim)
      (str/replace "{{git-count-revs}}" (b/git-count-revs nil))))

(defn make-opts [opts]
  (let [version (make-version)
        lib 'io.github.noahtheduke/lazytest]
    (assoc opts
      :lib lib
      :version version
      :scm {:url "https://github.com/noahtheduke/lazytest" :tag (str "v" version)}
      :main 'lazytest.main
      :basis (b/create-basis {:project "deps.edn"})
      :provided (b/create-basis {:project "deps.edn"
                                 :aliases [:provided]})
      :class-dir "target/classes"
      :jar-file (str (io/file "target" (format "%s-%s.jar" (name lib) version)))
      :uber-file (str (io/file "target" (format "%s-%s-standalone.jar" (name lib) version)))
      :src-dirs ["src/clojure"]
      :resource-dirs ["resources"]
      :java-src-dirs ["src/java"]
      :javac-opts ["--release" "11"]
      :pom-data [[:licenses
                  [:license
                   [:name "EPL-1.0"]
                   [:url "https://www.eclipse.org/legal/epl-v10.html"]]]])))

(defn clean [opts]
  (let [opts (make-opts opts)]
    (b/delete {:path (:class-dir opts)})))

(defn copy-src [opts]
  (let [opts (make-opts opts)]
    (b/copy-dir {:src-dirs (concat (:src-dirs opts) (:resource-dirs opts))
                 :target-dir (:class-dir opts)})))

(defn compile-java [opts]
  (println "Compiling" (str/join ", " (:java-src-dirs opts)))
  (b/javac {:src-dirs (:java-src-dirs opts)
            :class-dir (:class-dir opts)
            :basis (:basis opts)
            :javac-opts (:javac-opts opts)}))

(defn compile-clojure [opts]
  (let [opts (make-opts opts)]
    (when (:clean opts)
      (clean opts)
      (compile-java opts))
    (println "Compiling src/clojure")
    (let [out (-> opts
                  (assoc :basis (:provided opts)
                         :binding {#'clojure.core/*warn-on-reflection* true}
                         :out :capture
                         :err :capture)
                  (b/compile-clj)
                  (update :out str)
                  (update :err str))]
      (when (str/includes? (:err out)
                           "Reflection warning")
        (throw (ex-info (:err out) out))))))

(defn jar [opts]
  (let [opts (make-opts opts)]
    (clean opts)
    (copy-src opts)
    (compile-java opts)
    (b/jar opts)
    (println "Created" (str (b/resolve-path (:jar-file opts))))))

(defn write-pom
  [opts]
  (let [pom-path (b/pom-path opts)
        opts (assoc opts :pom-path pom-path)]
    (clein.pom/write-pom opts)))

(defn uberjar [opts]
  (let [opts (make-opts opts)]
    (clean opts)
    (copy-src opts)
    (compile-java opts)
    (write-pom opts)
    (compile-clojure opts)
    (b/uber opts)
    (println "Created" (str (b/resolve-path (:jar-file opts))))))

(defn deploy [opts]
  (let [opts (make-opts opts)]
    (clean opts)
    (copy-src opts)
    (compile-java opts)
    (write-pom opts)
    (b/jar opts)
    (dd/deploy {:installer :remote
                :artifact (b/resolve-path (:jar-file opts))
                :pom-file (b/pom-path opts)})))

(defn install [opts]
  (let [opts (make-opts opts)]
    (clean opts)
    (jar opts)
    (b/install opts)
    (println "Installed version" (:lib opts) (:version opts))))
