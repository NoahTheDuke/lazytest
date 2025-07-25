# Lazytest: A standalone test framework for Clojure

[![Clojars Project](https://img.shields.io/clojars/v/io.github.noahtheduke/lazytest.svg)](https://clojars.org/io.github.noahtheduke/lazytest)
[![cljdoc badge](https://cljdoc.org/badge/io.github.noahtheduke/lazytest)](https://cljdoc.org/d/io.github.noahtheduke/lazytest)

An alternative to `clojure.test`, aiming to be feature-rich and easily extensible.

## Table of Contents

<!-- toc -->

- [Getting Started](#getting-started)
- [Why a new test framework?](#why-a-new-test-framework)
- [Why no Clojurescript/other flavors?](#why-no-clojurescriptother-flavors)
- [Usage](#usage)
- [Writing tests with Lazytest](#writing-tests-with-lazytest)
- [Partitioning Individual Tests and Suites](#partitioning-individual-tests-and-suites)
- [Setup and Teardown](#setup-and-teardown)
- [Output](#output)
- [Doc Tests](#doc-tests)
- [Editor Integration](#editor-integration)
- [Run Lifecycle Overview](#run-lifecycle-overview)
- [Lazytest Internals](#lazytest-internals)
- [License](#license)

<!-- tocstop -->

## Getting Started

Add it to your deps.edn or project.clj:

```clojure lazytest/skip=true
{:aliases
 {:test {:extra-deps {io.github.noahtheduke/lazytest {:mvn/version "1.8.0"}}
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

## Why no Clojurescript/other flavors?

I am not nearly familiar enough with other flavors of Clojure and don't have the bandwidth to support them. If someone from the community is willing to steward that work, I'm open to adding support.

## Usage

With the above `:test` alias, call `clojure -M:test [options] [path...]` to run your test suite once, or `clojure -M:test --watch [options] [path...]` to use "Watch mode" (see below) to run repeatedly as files change. `[path...]` here means any file or directory. By default, Lazytest only runs tests found in the `test/` directory, but these can be passed in as additional arguments or with the `--dir` flag (see below):

Any of the below `[options]` can also be provided:

* `-d`, `--dir DIR`: Directory containing tests. Can be given multiple times.
* `-n`, `--namespace SYMBOL`: Run only the specified test namespaces. Can be given multiple times.
* `-v`, `--var SYMBOL`: Run only the specified fully-qualified symbol. Can be given multiple times.
* `-i`, `--include KEYWORD`: Run only test sequences or vars with this metadata keyword. Can be given multiple times.
* `-e`, `--exclude KEYWORD`: Exclude test sequences or vars with this metadata keyword. Can be given multiple times.
* `--output SYMBOL`: Output format. Can be given multiple times. (Defaults to `nested`.)
* `--md FILE`: Run doc tests in markdown file. Can be given multiple times. (See [Doc Tests](#doc-tests) below.)
* `--watch`: Runs under "Watch mode", which reloads and reruns your test suite as project or test code changes.
* `--delay NUM`: How many milliseconds to wait before checking for changes to reload. Only used in "Watch mode". (Defaults to 500.)
* `--help`: Print help information.
* `--version`: Print version information.

> [!NOTE]
> If both `--namespace` and `--var` are provided, then Lazytest will run all tests within the namespaces AND the specified vars. They are inclusive, not exclusive.

> [!NOTE]
> `--exclude` overrides `--include`, if both are provided.

### Watch mode

Watch mode uses [clj-reload](https://github.com/tonsky/clj-reload) to reload all local changes on the classpath, plus any files containing namespaces that depend on the changed files. Watch mode defaults to `lazytest.reporters/dots` to make the output easier to read. By default, it checks for changes once every 500 milliseconds (1/2 a second), but this can be changed with `--delay`. Watch mode supports all of the other options as well, so you can select a different output style, specific directories, test namespaces, or test varsto check, etc.

Type `CTRL-C` to stop.

## Writing tests with Lazytest

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

### Aliases

To help write meaningful tests, a couple aliases have been defined for those who prefer different vocabulary:

* `context` for `describe`
* `specify` for `it`
* `should` for `expect`

These can be used interchangeably:

```clojure
(require '[lazytest.core :refer [context specify should]])

(defdescribe context-test
  (context "with integers"
    (specify "that sums work"
      (should (= 7 (+ 3 4)) "follows basic math")
      (expect (not= 7 (+ 1 1))))))
```

There are a number of experimental namespaces that define other aliases, with distinct behavior, if the base set of vars don't fit your needs:

* [lazytest.experimental.interfaces.clojure-test][lazytest.clojure-test] to mimic `clojure.test`.
* [lazytest.experimental.interfaces.midje][lazytest.midje] to mimic [Midje](https://github.com/marick/midje).
* [lazytest.experimental.interfaces.qunit][lazytest.qunit] to mimic [QUnit](https://qunitjs.com/).
* [lazytest.experimental.interfaces.xunit][lazytest.xunit] to mimic a standard [xUnit](https://en.wikipedia.org/wiki/XUnit) framework.

[lazytest.clojure-test]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.experimental.interfaces.clojure-test
[lazytest.midje]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.experimental.interfaces.midje
[lazytest.qunit]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.experimental.interfaces.qunit
[lazytest.xunit]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.experimental.interfaces.xunit

### Var Metadata

In addition to finding the tests defined with `defdescribe`, Lazytest also checks all vars for `:lazytest/test` metadata. If the `:lazytest/test` metadata is a function, a test case, or a test suite, it's treated as a top-level `defdescribe` for the associated var and executed just like other tests. `:lazytest/test` functions are given the doc string ``"`:lazytest/test` metadata"``.

How to write them:

```clojure
(defn fn-example
  {:lazytest/test #(expect (= 1 1))}
  [])
(defn test-case-example
  {:lazytest/test (it "test case example docstring" (expect (= 1 1)))}
  [])
(defn describe-example
  {:lazytest/test
    (describe "top level docstring"
      (it "first test case" (expect (= 1 1)))
      (it "second test case" (expect (= 1 1))))}
  [])
```

How they're printed:

```
  lazytest.readme-test
    #'lazytest.readme-test/fn-example
      √ `:lazytest/test` metadata
    #'lazytest.readme-test/test-case-example
      √ test case example docstring
    #'lazytest.readme-test/describe-example
      top level docstring
        √ first test case
        √ second test case
```

These can get unweildy if multiple test cases are included before a given implementation, so I recommend either moving them to a dedicated test file or moving the `attr-map` to the end of the function definition:

```clojure
(defn post-attr-example
  ([a b]
   (+ a b))
  {:lazytest/test
   (describe "Should be simple addition"
     (it "handles ints"
       (expect (= 2 (post-attr-example 1 1))))
     (it "handles floats"
       (expect (= 2.0 (post-attr-example 1.0 1.0)))))})
```

> [!NOTE]
> Lazytest previously used `:test` metadata, but because `clojure.test` relies on that, it impeded having both `clojure.test` and Lazytest tests in a given codebase.

## Partitioning Individual Tests and Suites

All of the test suite and test case macros (`defdescribe`, `describe`, `it`, `expect-it`) take a metadata map after the docstring. Adding `:focus true` to this map will cause *only* that test/suite to be run. Removing it will return to the normal behavior (run all tests).

```clojure lazytest/skip=true
(defdescribe focus-test
  (it "will be run"
    {:focus true}
    (expect (= 1 2)))
  (it "will be skipped"
    (expect (= 1 1))))
```

And adding `:skip true` to the metadata map will cause that test/suite to be *not* run:

```clojure lazytest/skip=true
(defdescribe skip-test
  (it "will be skipped"
    {:skip true}
    (expect (= 1 2)))
  (it "will be run"
    (expect (= 1 1))))
```

> [!NOTE]
> `:skip` overrides `:focus`, so `{:focus true :skip true}` will be skipped.

Additionally, you can use the cli option `-n`/`--namespace` to specify one or more namespaces to focus wholly, or you can use the cli option `-v`/`--var` to specify one or more fully-qualified vars to focus. This allows for testing from the command line without modifying source files.

To partition your test suite based on metadata, you can use `-i`/`--include` to only run tests with the given metadata, or `-e`/`--exclude` to skip tests with the given metadata.

## Setup and Teardown

To handle set up and tear down of stateful architecture, Lazytest provides the hook macros `before`, `before-each`, `after-each`, `after`, and `around`, along with the helper function `set-ns-context!`. You can call them directly in a `describe` block or add them to a `:context` vector in suite metadata. (To read a more specific description of how this works, please read the section titled `Run Lifecycle Overview`.)

```clojure
(require '[lazytest.core :refer [expect-it before before-each after-each after around]])

(defdescribe before-and-after-test
  (let [state (volatile! [])]
    (describe "before and after example"
      (before (vswap! state conj :before))
      (after (vswap! state conj :after))
      (expect-it "can do side effects" (vswap! state conj :expect)))
    (describe "results"
      (expect-it "has been properly tracked"
        (= [:before :expect :after] @state)))))

(defdescribe around-test
  (let [state (volatile! [])]
    (describe "around example"
      {:context [(around [f]
                   (vswap! state conj :around-before)
                   (f)
                   (vswap! state conj :around-after))]}
      (expect-it "can do side effects" (vswap! state conj :expect)))
    (describe "results"
      (expect-it "correctly ran the whole thing"
        (= [:around-before :expect :around-after] @state)))))

(defdescribe each-test
  (let [state (volatile! [])]
    (describe "each examples"
      {:context [(before (vswap! state conj :before))
                 (before-each (vswap! state conj :before-each))]}
      (expect-it "can do side effects" (vswap! state conj :expect-1))
      (expect-it "can do side effects" (vswap! state conj :expect-2)))
    (expect-it "has been properly tracked"
      (= [:before :before-each :expect-1 :before-each :expect-2] @state))))
```

`around` hooks are combined with the same logic as `clojure.test`'s `join-fixtures`.

Context functions of the same kind are run in the order they're defined. When executing a given suite or test-case, all `before` hooks are run once, then each `before-each` hook is run, then the `around` hooks are called on the nested tests (if they exist), then each `after-each` hook is run, then all `after` hooks are run once.

To set context functions for an entire namespace, use `set-ns-context!`. There is currently no way to define run-wide context functions.

In `clojure.test`, `(use-fixtures :each ...)` will set the provided fixtures to wrap each test var. To achieve the same in Lazytest, define a var of the target hook and add it to the `defdescribe`'s `:context` block of each var in the namespace. This is necessarily more tedious than `use-fixtures`, but it is also more explicit and gracefully handles special cases (define multiple functions to handle subtle differences, use whichever is situationally helpful).

```clojure lazytest/skip=true
(defonce ^:dynamic *db-connection* nil)
(def prep-db
  (around [f]
    (binding [*db-connection* (get-db-connection ...)]
      (f))))

(defdescribe needs-a-db-test
  {:context [prep-db]}
  (it "has the right connection"
    (expect (= 1 (count (sql/query *db-connection* "SELECT * FROM users;"))))))
```

> [!IMPORTANT]
> Because `describe` blocks are eagerly evaluated (returning a test suite map) whereas `it` blocks wrap the body in a no-arg function that is called by the runner, binding forms such as `let` will happen before any context calls are evaluated. This can lead to unintuitive results and hard-to-understand errors.
>
> For example, if we change the above code example to bind the `*db-connection*` dynamic variable to a local variable outside of an `it` block, the binding will happen immediately (setting `db-conn` to `nil`). When the runner actually runs `needs-a-db-test`, the `around` context function will set `*db-connection*` but the test will have already closed over `db-conn` in its existing state (`nil`) and thus the test will fail.
>
> ```clojure
> (defonce ^:dynamic *db-connection* nil)
> (def prep-db
>   (around [f]
>     (binding [*db-connection* (get-db-connection ...)]
>       (f))))
> 
> (defdescribe needs-a-db-test
>   {:context [prep-db]}
>   (let [db-conn *db-connection*]
>     (it "has the right connection"
>       (expect (= 1 (count (sql/query db-conn "SELECT * FROM users;")))))))
> ```

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

## Doc Tests

Lazytest can run tests in code blocks of your markdown files with `--md FILE`. It looks for any triple backtic-delimited code block that has `clojure` or `clj` as the language specifier, and that doesn't have `lazytest/skip=true` in the info-string, bundles it into a standalone `describe` block, and then runs all of the suites as a single suite under the name of the markdown file.

It determines what should be considered a test (`(expect (= x y))`) by the presence of `=>`. Code immediately before a line containing `=>` (leading `;` optional) is treated as the actual, and the value after treated as the expected result.

This will run:

````markdown
```clojure
(defn adder [a b]
  (+ a b))

(adder 5 6)
;; => 11
```
````

Whereas these will not (first is skipped, second isn't "clojure" or "clj"):

````markdown
```clojure lazytest/skip=true
(System/exit 1)
;; => exit!!!
```

```clojurescript
print("Hello world!")
```
````

Additionally, a custom string can be used instead of the default (headers from the markdown file) by using the info-string `lazytest/describe`:


````markdown
```clojure lazytest/describe=easy-adder
(+ 5 6)
;; => 11
```
````

will be printed as:

```markdown
  readme-md
    easy-adder
      √ Doc Tests
```

## Editor Integration

The entry-points are at `lazytest.repl`: `run-all-tests`, `run-tests`, and `run-test-var`. The first runs all loaded test namespaces, the second runs the provided namespaces (either a single namespace or a collection of namespaces), and the third runs a single test var. If your editor can define custom repl functions, then it's fairly easy to set these as your test runner.

### Neovim

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

### VSCode

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

### IntelliJ

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

## Run Lifecycle Overview

This is inspired by [Mocha](https://mochajs.org)'s excellent documentation.

### From the CLI

1. A user runs Lazytest, either through leiningen or Clojure CLI.
2. Lazytest parses the command line arguments to determine the relevant configuration.
3. Lazytest finds test files. If the user provides `--dir`, then every file in the file trees of all given directories are checked. Otherwise, all files within the `test` directorie are checked.
4. Lazytest loads all test files. Using `tools.namespace`, the namespace of each `.clj` is extracted and `require`d, which creates the necessary vars.
5. Lazytest gathers all test vars from the required namespaces. It checks each var in each namespace against the following list of questions.
    1. Is the var defined with `defdescribe`? Call the `defdescribe`-constructed function and use the result.
    2. Does the var point to a `suite`? Resolve the var and use the result.
    3. Does the var have `:lazytest/test` metadata that is either a suite (`describe`) or a test case (`it`)? Create a new suite with `describe` and set the `:lazytest/test` metadata as a child.
    4. Does the var have `:lazytest/test` metadata that is a function? Create a new suite with `describe`, create a new test case with `it`, and then set the docstring for the test case to `:lazytest/test metadata`, and the body to calling the `:lazytest/test` metadata function.
6. Lazytest groups each namespace into a `:lazytest/ns` suite, and then groups all of the namespace suites into a `:lazytest/run` suite.
7. Lazytest does a depth-first walk of the run suite, filtering nses by `--namespace`, vars by `--var`, and all suites and test cases by `--include` or `--exclude` (with `:focus` being automatically included). These are prioritized as such:
    1. `--namespace` narrows all namespaces to those that exactly match. The namespaces of `--var` vars are included as well. If `--namespace` is not provided, all namespaces are selected.
    2. `--var` narrows all vars from the selected namespaces. If `--namespace` is provided, all vars from those namespaces are selected as well. If `--var` is not provided, all vars are selected.
    3. The suite for each var is selected by selecting all `--include` or `:focus` metadata suites and tests cases and then removing all `--exclude` suites and test cases. If no suites or test cases have `:focus` metadata or `--include` hasn't been provided, then everything is selected. (To be clear, `--exclude` overrides `:focus` and `--include`.)
8. Lazytest calls the runner on the filtered run suite.
    * For suites:
        1. Run each `before` hook.
        2. If there are any `around` hooks, combine them with `clojure.test/join-fixtures`, and then execute the next step in a thunk wrapped in the combined `around` function.
        3. For each child in `:children`, restart from step 1 of the appropriate sequence.
        4. Run each `after` hook.
    * For test cases:
        1. Run each `before-each` hook (including from all parents), outermost first, in definition order.
        2. Execute the test function, get the `test-case-result`.
        3. Run each `after-each` hook (including from all parents), innermost first, in definition order.
9. Depending on the chosen reporter, Lazytest prints the results of each suite and test case immediately or at another point.
10. The run is ended with `System/exit`, and the exit value is either `0` for no failures or `1` for any number of failures.

### Programmatically

The process is roughly the same as from the CLI, but with CLI-specific steps skipped.

1. Build a suite.
    * If using `lazytest.repl/run-tests`, the specified namespace used as the required namespace.
    * If using `lazytest.repl/run-all-tests`, all currently loaded are used (found with `clojure.core/all-ns`).
    * If using `lazytest.repl/run-test-var`, the single var is used as the suite.
2. If not given a var, step 5 is executed as described above to produce a suite.
3. Steps 7-9 are executed as described above on the suite, with the note that only `:focus` is considered when filtering.
4. The results from the run are summarized and returned to the caller.

## Lazytest Internals

The smallest unit of testing is a *test case* (see `lazytest.test-case/test-case`). When the `:body` function is called, it may throw an exception to indicate failure. If it does not throw an exception, it is assumed to have passed. The return value of a test case is always ignored. Running a test case may have side effects.

> [!NOTE]
> The macros `lazytest.core/it` and `lazytest.core/expect-it` create test cases.

Tests cases are organized into *suites* (see `lazytest.suite/suite`). A suite has `:children`, which is a sequence, possibly lazy, of test cases and/or test suites. Suites, therefore, may be nested inside other suites, but nothing may be nested inside a test case.

> [!NOTE]
> The macro `lazytest.core/describe` creates a test suite. The macro `lazytest.core/defdescribe` creates a no-argument function that returns a test suite.

A test suite body SHOULD NOT have side effects; it is only used to generate test cases and/or other test suites.

The test *runner* is responsible for gathering suites (see `lazytest.find/find-suite` and `lazytest.filter/filter-tree`) and running test cases (see `lazytest.test-case/try-test-case`). It may also provide feedback on the success of tests as they run.

The test runner also returns a sequence of *results*, which are either *suite results* (see `lazytest.suite/suite-result`) or *test case results* (see `lazytest.test-case/test-case-result`). That sequence of results is passed to a *reporter*, which formats results for display to the user. Multiple reporters are provided, see the namespace `lazytest.reporters`.

## License

Originally by [Alessandra Sierra](https://www.lambdasierra.com).

Currently developed by [Noah Bogart](https://github.com/NoahTheDuke).

Licensed under [Eclipse Public License 1.0](https://www.eclipse.org/org/documents/epl-v10.html)
