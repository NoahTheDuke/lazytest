# Installation

Add it to your deps.edn or project.clj:

## Clojure CLI

```clojure lazytest/skip=true
{:aliases
 {:test {:extra-deps {io.github.noahtheduke/lazytest {:mvn/version "2.1.0"}}
         :extra-paths ["test"]
         :main-opts ["-m" "lazytest.main"]}}}
```

Run with `clojure clojure -M:test [args...]`.

## Leiningen

Add this to `project.clj`:

```clojure lazytest/skip=true
:profiles {:dev {:dependencies [[io.github.noahtheduke/lazytest "2.1.0"]]}}
:aliases {"test" ["run" "-m" "lazytest.main"]}
```

Run with `leiningen test [args...]`.
