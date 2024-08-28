# Lazytest: A standalone test framework for Clojure

[![Clojars Project](https://img.shields.io/clojars/v/io.github.noahtheduke/lazytest.svg)](https://clojars.org/io.github.noahtheduke/lazytest)
[![cljdoc badge](https://cljdoc.org/badge/io.github.noahtheduke/lazytest)](https://cljdoc.org/d/io.github.noahtheduke/lazytest)

An alternative to `clojure.test`, aiming to be feature-rich and easily extensible.

## Getting Started

Add it to your deps.edn or project.clj:

```clojure
{:aliases
 {:test {:extra-deps {io.github.noahtheduke/lazytest {:mvn/version "0.4.1"}}
         :extra-paths ["test"]
         :main-opts ["-m" "lazytest.main"]}}}
```

In a test file:

```clojure
(ns example.readme-test
  (:require [lazytest.core :refer [defdescribe describe expect it]]))

(defdescribe seq-fns-test
  (describe keep
    (it "should reject nils"
      (expect (= '(1 2 3) (keep identity [nil 1 2 3]))))
    (it "should return a sequence"
      (expect (seq? (seq (keep identity [nil])))))))
```

From the command line:

```bash
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

## Usage

With the above `:test` alias, you run with `clojure -M:test [options]` where `[options]` are any of the below.

* `-d`, `--dir DIR`: Directory containing tests. (Defaults to `test`.)
* `-n`, `--namespace NS-SYM`: Run only the specified test namespaces. Can be given multiple times.
* `-v`, `--var VAR-SYM`: Run only the specified fully-qualified symbol.
* `-i`, `--include KEYWORD`: Run only test sequences or vars with this metadata keyword.
* `-e`, `--exclude KEYWORD`: Exclude test sequences or vars with this metadata keyword.
* `--output OUTPUT`: Output format. Can be given multiple times. (Defaults to `nested`.)
* `--help`: Print help information.
* `--version`: Print version information.

Note: If both `--namespace` and `--var` are provided, then Lazytest will run all tests within the namespaces AND the specified vars. They are inclusive, not exclusive.

Note: `--exclude` overrides `--include`, if both are provided.

## Writing tests with 'lazytest'

The primary api is found in `lazytest.core` namespace. It mimics the behavior-driven testing style popularized by libraries such as [RSpec](https://rspec.info/) and [Mocha](https://mochajs.org).

Define tests with `defdescribe`, group test suites and test cases together into a suite with `describe`, and define test cases with `it`. `describe` can be nested. `defdescribe`'s docstring is optional, `describe` and `it`'s docstrings are not.

```clojure
(defdescribe +-test "with integers"
  (it "computes the sum of 1 and 2"
    (expect (= 3 (+ 1 2))))
  (it "computes the sum of 3 and 4"
    (assert (= 7 (+ 3 4)))))
```

The `expect` macro is like `assert` but carries more information about the failure, such as the given form, the returned value, and the location of the call. It throws an exception if the expression does not evaluate to logical true.

If an `it` runs to completion without throwing an exception, the test case is considered to have passed.

### Var Metadata

In addition to finding the tests defined with `defdescribe`, Lazytest also checks all vars for `:test` metadata. If the `:test` metadata is a function, a test case, or a test suite, it's treated as a top-level `defdescribe` for the associated var and executed just like other tests. `:test` functions are given the doc string ``"`:test` metadata"``.

How to write them:

```clojure
(ns example.metadata-test ...)

(defn fn-example {:test #(expect ...)})
(defn test-case-example {:test (it "test case example docstring" ...)})
(defn suite-example {:test (suite ...)})
(defn describe-example {:test (describe "top level docstring" ...)})
```

How they're printed:

```
  example.metadata-test
    #'example.metadata-test/fn-example
      √ `:test` metadata
    #'example.metadata-test/test-case-example
      √ test case example docstring
    #'example.metadata-test/suite-example
      √ first test case
      √ second test case
    #'example.metadata-test/describe-example
      top level docstring
        √ third test case
        √ fourth test case
```

These can get unweildy if multiple test cases are included before a given implementation, so I recommend either moving them to a dedicated test file or moving the `attr-map` to the end of the function definition:

```clojure
(defn describe-example
  ([a b]
   (+ a b))
  {:test (describe "Should be simple addition"
           (it "handles ints"
             (expect (= 2 (describe-example 1 1))))
           (it "handles floats"
             (expect (= 2.0 (describe-example 1.0 1.0)))))})
```

## Focusing on Individual Tests and Suites

All of the test suite and test case macros (`defdescribe`, `describe`, `it`, `expect-it`) take a metadata map after the docstring. Adding `:focus true` to this map will cause *only* that test/suite to be run. Removing it will return to the normal behavior (run all tests).

```clojure
(defdescribe my-test
  "fancy test"
  {:focus true}
  ...)
```

Additionally, you can use the cli option `-n`/`--namespace` to specify one or more namespaces to focus wholly, or you can use the cli option `-v`/`--var` to specify one or more fully-qualified vars to focus. This allows for testing from the command line without modifying source files.

To partition your test suite based on metadata, you can use `-i`/`--include` to only run tests with the given metadata, or `-e`/`--exclude` to skip tests with the given metadata.

## Output

Lazytest comes with a number of reporters built-in. These print various information about the test run, both as it happens and surrounding execution. They are specified at the cli with `--output` and can be simple symbols or fully-qualified symbols. If a custom reporter is provided, it must be fully-qualified. (Otherwise, Lazytest will try to resolve it to the `lazytest.reporters` namespace and throw an exception.)

### `lazytest.reporters/nested`

The default Lazytest reporter. Inspired heavily by [Mocha's Spec][mocha spec] reporter, it prints each suite and test case indented as they are written in the test files.

[mocha spec]: https://mochajs.org/#spec


```
  lazytest.core-test
    it-test
      √ will early exit
      √ arbitrary code
    with-redefs-test
      redefs inside 'it' blocks
        × should be rebound FAIL
      redefs outside 'it' blocks
        √ should not be rebound

lazytest.core-test
  with-redefs-test
    redefs inside 'it' blocks
      should be rebound:

this should be true
Expected: (= 7 (plus 2 3))
Actual: false
Evaluated arguments:
 * 7
 * 6
Only in first argument:
7
Only in second argument:
6

in lazytest/core_test.clj:29

Ran 90 test cases in 0.06548 seconds.
1 failure.
```

### `lazytest.reporters/dots`

A minimalist reporter. Prints passing test cases as green `.` and failures as red `F` during the test run. Test suites are grouped with parentheses (`(`/`)`). It also prints the failure results and summary as in `lazytest.reporters/nested`, which has been elided below for brevity.

```
(...)(..F................)(.....)(..)(..)(....)(........)(........................................)(.......)
```

### `lazytest.reporters/clojure-test`

Mimics `clojure.test`'s default reporter, treating suite and test-case docstrings as testing strings.

```
Testing lazytest.core-test

FAIL in (with-redefs-test) (lazytest/core_test.clj:29)
with-redefs-test redefs inside 'it' blocks should be rebound
this should be true
expected: (= 7 (plus 2 3))
  actual: false

Ran 25 tests containing 90 test cases.
1 failure, 0 errors.
```

### `lazytest.reporters/quiet`

Prints nothing. Useful if all you want is the return code.

### `lazytest.reporters/debug`

Prints loudly about every step of the run. Incredibly noise, not recommended for anything other than debugging Lazytest internals.

## Editor Integration

The entry-points are at `lazytest.repl`: `run-all-tests`, `run-tests`, and `run-test-var`. The first runs all loaded test namespaces, the second runs the provided namespaces (either a single namespace or a collection of namespaces), and the third runs a single test var. If your editor can define custom repl functions, then it's fairly easy to set these as your test runner.

### Example configuration

Neovim with [Conjure](https://github.com/Olical/conjure):

```lua
-- in your init.lua
local runners = require("conjure.client.clojure.nrepl.action")
runners["test-runners"].lazytest = {
  ["namespace"] = "lazytest.repl",
  ["all-fn"] = "run-all-tests",
  ["ns-fn"] = "run-tests",
  ["single-fn"] = "run-test-var",
  ["default-call-suffix"] = "",
  ["name-prefix"] = "#'",
  ["name-suffix"] = ""
}
vim.g["conjure#client#clojure#nrepl#test#runner"] = "lazytest"
```

VSCode with [Calva](https://calva.io/custom-commands):

```json
"calva.customREPLCommandSnippets": [
    {
        "name": "Lazytest: Test All Tests",
        "snippet": "(lazytest.repl/run-all-tests)"
    },
    {
        "name": "Lazytest: Test Current Namespace",
        "snippet": "(lazytest.repl/run-tests $editor-ns)"
    },
    {
        "name": "Lazytest: Test Current Var",
        "snippet": "(lazytest.repl/run-test-var #'$top-level-defined-symbol)"
    }
],
```

IntelliJ with [Cursive](https://cursive-ide.com/userguide/repl.html#repl-commands):

```
Name: Lazytest - Test All Tests
Execute Command: (lazytest.repl/run-all-tests)
Execution Namespace: Execute in current file namespace
Results: Print results to REPL output

Name: Lazytest - Test Current Namespace
Execute Command: (lazytest.repl/run-tests ~file-namespace)
Execution Namespace: Execute in current file namespace
Results: Print results to REPL output

Name: Lazytest - Test Current Var
Execute Command: (lazytest.repl/run-test-var #'~current-var)
Execution Namespace: Execute in current file namespace
Results: Print results to REPL output
```

## Lazytest Internals

The smallest unit of testing is a *test case*, which is a function (see `lazytest.test-case/test-case`). When the function is called, it may throw an exception to indicate failure. If it does not throw an exception, it is assumed to have passed. The return value of a test case is always ignored. Running a test case may have side effects.

NOTE: The macros `lazytest.describe/it` and `lazytest.describe/expect-it` create test cases.

Tests cases are organized into *suites*. A test suite is a function (see `lazytest.suite/suite`) that returns a *test sequence*. A test sequence (see `lazytest.suite/test-seq`) is a sequence, possibly lazy, of test cases and/or test suites. Suites, therefore, may be nested inside other suites, but nothing may be nested inside a test case.

NOTE: The macros `lazytest.describe/defdescribe` and `lazytest.describe/describe` create test suites.

A test suite function SHOULD NOT have side effects; it is only used to generate test cases and/or other test suites.

A test *runnner* is responsible for expanding suites (see `lazytest.suite/expand-suite`) and running test cases (see `lazytest.test-case/try-test-case`). It may also provide feedback on the success of tests as they run.

The test runner also returns a sequence of *results*, which are either *suite results* (see `lazytest.suite/suite-result`) or *test case results* (see `lazytest.test-case/test-case-result`). That sequence of results is passed to a *reporter*, which formats results for display to the user. Multiple reporters are provided, see the namespace `lazytest.reporters`.

## License

Originally by [Alessandra Sierra](https://www.lambdasierra.com).

Currently developed by [Noah Bogart](https://github.com/NoahTheDuke).

Licensed under [Eclipse Public License 1.0](https://www.eclipse.org/org/documents/epl-v10.html)
