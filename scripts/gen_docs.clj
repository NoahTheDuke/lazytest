(ns gen-docs
  (:require
    [babashka.fs :as fs]
    [babashka.process :as p]))

(doseq [f (concat ["README.md"] (fs/glob "docs" "**.md"))]
  (println (str f))
  (p/shell "markdown-toc" "-i" "--maxdepth" "3" (str f)))
