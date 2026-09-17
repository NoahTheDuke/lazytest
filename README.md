# Lazytest: A standalone test framework for Clojure

[![Clojars Project](https://img.shields.io/clojars/v/io.github.noahtheduke/lazytest.svg)](https://clojars.org/io.github.noahtheduke/lazytest)
[![cljdoc badge](https://cljdoc.org/badge/io.github.noahtheduke/lazytest)](https://cljdoc.org/d/io.github.noahtheduke/lazytest)

An alternative to `clojure.test`, aiming to be feature-rich and easily extensible.

## Table of Contents

<!-- toc -->

- [Getting Started](#getting-started)
- [Why a new test framework?](#why-a-new-test-framework)
- [Supported dialects](#supported-dialects)
- [License](#license)

<!-- tocstop -->

## Getting Started

More explicit instructions can be found in the [installation][installation], [usage][usage], and [configuration][configuration] pages, but here's a quick rundown:

[installation]: docs/installation.md
[usage]: docs/usage.md
[configuration]: docs/configuration.md

Add it to your deps.edn or project.clj:

```clojure lazytest/skip=true
{:aliases
 {:test {:extra-deps {io.github.noahtheduke/lazytest {:mvn/version "2.1.0"}}
         :extra-paths ["test"]
         :main-opts ["-m" "lazytest.main"]}}}
```

In a test file, import with:

```clojure
(require '[lazytest.core :refer [defdescribe describe expect it]])
```

And then write a simple test:

```clojure lazytest/skip=true
(defdescribe seq-fns-test
  (describe keep
    (it "should reject nils"
      (expect (= '(1 2 3) (keep identity [nil 1 2 3]))))
    (it "should return a sequence"
      (expect (seq? (seq (keep identity [nil])))))))
```

From the command line:

```
$ clojure -M:test

  lazytest.readme-test
    seq-fns-test
      #'clojure.core/keep
        √ should reject nils
        × should return a sequence FAIL

lazytest.readme-test
  seq-fns-test
    #'clojure.core/keep
      should return a sequence:

Expectation failed
Expected: (seq? (seq (keep identity [nil])))
Actual: nil
Evaluated arguments:
 * ()

in lazytest/readme_test.clj:11

Ran 2 test cases in 0.00272 seconds.
1 failure.
```

## Why a new test framework?

`clojure.test` has existed since 1.1 and while it's both ubiquitous and useful, it has a number of [problems][problems]:
* `is` tightly couples running test code and reporting on it.
* `are` is strictly worse than `doseq` or `mapv`.
* `clojure.test/report` is `^:dynamic`, but that leads to being unable to combine multiple reporters at once, or libraries such as Leiningen monkey-patching it.
* Tests can't be grouped or bundled in any meaningful way (no, defining `test-ns-hook` does not count).
* `testing` calls aren't real contexts, they're just strings.
* Fixtures serve a real purpose but because they're set on the namespace, their definition is side-effecting and using them is complicated and hard to reuse.

[problems]: https://stuartsierra.com/2010/07/05/lazytest-status-report

There exists very good libraries like [Expectations v2][expectations v2], [kaocha][kaocha], [eftest][eftest], Nubank's [matcher-combinators][nmc], Cognitect's [test-runner][ctr] that improve on `clojure.test`, but they're all still built on a fairly shaky foundation. I think it's worthwhile to explore other ways of being, other ways of doing stuff. Is a library like lazytest good? is a testing framework like this good when used in clojure? I don't know, but I'm willing to try and find out.

[expectations v2]: https://github.com/clojure-expectations/clojure-test
[kaocha]: https://github.com/lambdaisland/kaocha
[eftest]: https://github.com/weavejester/eftest
[nmc]: https://github.com/nubank/matcher-combinators
[ctr]: https://github.com/cognitect-labs/test-runner

Other alternatives such as [Midje][midje], [classic Expectations][expectations v1], and [speclj][speclj] attempted to correct some of those issues and they made good progress. However, some (such as `Midje`) relied on non-list style (`test => expected`) and most don't worked well with modern repl-driven development practices (as seen by the popularity of the aforementioned clojure.test-compatible [Expectations v2][expectations v2]).

[expectations v1]: https://github.com/clojure-expectations/expectations
[midje]: https://github.com/marick/midje
[speclj]: https://github.com/slagyr/speclj

I like the ideas put forth in Alessandra's post above about Lazytest and hope to experiment with achieving them 14 years later, while borrowing heavily from the work in both the Clojure community and test runners frameworks in other languages.

## Supported dialects

Lazytest supports Clojure (1.10+) and Babashka (v1.12.208+). It does not support Clojurescript or other flavors at this time.

## License

Originally by [Alessandra Sierra](https://www.lambdasierra.com).

Currently developed by [Noah Bogart](https://github.com/NoahTheDuke).

Licensed under [Eclipse Public License 1.0](https://www.eclipse.org/org/documents/epl-v10.html)
