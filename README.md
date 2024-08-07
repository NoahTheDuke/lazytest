# Lazytest: A new test framework for Clojure

An alternative to `clojure.test`, aiming to be feature-rich and easily extensible.

## Getting Started

Add it to your deps.edn or project.clj:

```clojure
{:aliases
 {:test {:extra-deps {io.github.noahtheduke/lazytest {:mvn/version "0.3.0"}}
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
      (expect (= '(1 false 2 3) (keep any? [nil 1 false 2 3]))))
    (it "should return a sequence"
      (expect (seq (keep any? []))))))
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
Expected: (seq (keep any? []))
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

Lazytest comes with a number of reporters built-in. These print various information about the test run, both as it happens and surrounding execution. It's generally best to use one of the full reporters, to see all relevant information.

Reporter pieces:

* **focused**: Prints a message when tests are focused at the start of the test run.
* **summary**: Prints the number of test cases and failures at the end of the test run.
* **results**: Prints the failed assertions, their arguments, and associated information at the end of the test run.
* **nested\***: Prints each suite and test case on a new line, indenting at each suite, during the test run.
* **dots\***: Prints passing tests as `.` and failures as `F` during the test run. Test cases in namespace are wrapped in parentheses.
* **profile**: Prints the slowest 5 namespaces and slowest 5 test vars by duration at the end of the test run.

Full reporters:

* **nested**: Runs `focused`, `nested*`, `results`, and `summary`. This is the default Lazytest reporter.
* **dots**: Runs `focused`, `dots*`, `results`, and `summary`.
* **clojure-test**: Mimics `clojure.test`'s default reporter, treating suite and test-case docstrings as testing strings.

Specialty reporters:

* **quiet**: Prints nothing. Useful if all you want is the return code.
* **debug**: Prints loudly about every step of the run. Incredibly noise, not recommended for anything other than debugging Lazytest internals.

If multiple reporters are specified with `--output`, then they are run in the order chosen whenever they would print. For example, using `-o summary -o results` will print `Ran 5 test cases in 0.12346 seconds.` before printing `example test-case: Expectation Failed`. This is only important when multiple reporters print at the same step in the run, but it can be the difference between information being obvious or hidden.

TODO: Show examples in `docs/output.md`.

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
