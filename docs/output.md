# Output

Lazytest comes with a number of reporters built-in. These print various information about the test run, both as it happens and surrounding execution. They are specified at the cli with `--output` and can be simple symbols or fully-qualified symbols. If a custom reporter is provided, it must be fully-qualified. (Otherwise, Lazytest will try to resolve it to the `lazytest.reporters` namespace and throw an exception.)

<!-- toc -->

- [Built-in Reporters](#built-in-reporters)
  * [`lazytest.reporters/nested`](#lazytestreportersnested)
  * [`lazytest.reporters/dots`](#lazytestreportersdots)
  * [`lazytest.reporters/clojure-test`](#lazytestreportersclojure-test)
  * [`lazytest.reporters/quiet`](#lazytestreportersquiet)
  * [`lazytest.reporters/debug`](#lazytestreportersdebug)
- [Writing Custom Reporters](#writing-custom-reporters)

<!-- tocstop -->

## Built-in Reporters

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

## Writing Custom Reporters

The requirements for writing a custom reporter is fairly simple: It needs to be an `ifn` that accepts 2 arguments, as it will be called by the runner on the run's `config` and the suite or test-case or result at that moment.

All of the built-in reporters are multimethods, as that provides the easiest means of handling each type object to be reported on. The objects are maps that have appropriate `:type` entries, and the public var `lazytest.reporters/reporter-dispatch` exists to make this easy: `(defmulti nested* {:arglists '([config m])} #'reporter-dispatch)`. Each step in the run generally has a pair of types, one before the current suite/case is run and one after, with an additional `:results` holding the nested results from the children or the test case's result.

Here is a list of the types, along with info about when they are called and what the object might contain:

* `:begin-test-run`, `:end-test-run`: The top-level suite, parent to all test suites and cases that will be executed this run.
* `:begin-test-ns`, `:end-test-ns`: A suite for a given namespace, typically parent to all of the test vars in that namespace.
* `:begin-test-var`, `:end-test-var`: A suite for a single test var (typically from a `defdescribe`).
* `:begin-test-suite`, `:end-test-suite`: A stand-alone suite (typically from a `describe`).
* `:begin-test-case`, `:end-test-case`: A single test case (typically from an `it`), before it has been run.

Additionally, the test case's result will be reported between `:begin-test-case` and `:end-test-case`:

* `:pass`: The test case passed successfully.
* `:fail`: The test case failed for some reason.
