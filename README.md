# Lazytest: A new test framework for Clojure

An alternative to `clojure.test`, aiming to be feature-rich and easily extensible.

## Why a new test framework?

> Isn't `clojure.test` good enough?

Lazytest is designed to be a standalone test system for Clojure, disconnected from the
built-in `clojure.test`. `clojure.test` has existed since 1.1 and while it's both
ubiquitous and useful, it has a number of
[problems](https://stuartsierra.com/2010/07/05/lazytest-status-report).

Other alternatives such as `Midje` and `expectations` have attempted to correct some of
those issues and they made good progress, but many (such as `Midje`) relied on non-list
style (`test => expected`) and most don't worked well with modern repl-driven
development practices (using `gynsym`s instead of named test vars).

I like the ideas put forth in Sandra's post above about Lazytest and hope to experiment
with achieving them 10 years later.

## Testing with 'describe'

The `lazytest.describe` namespace mimics the behavior-driven testing
style popularized by libraries such as [RSpec](http://rspec.info).

Use the `describe` macro to create a group of tests. Start the group
with a name and an optional documentation string.

```clojure
(ns examples.readme.groups
  (:require [lazytest.describe :refer [describe it]]))

(describe app-test "This application" ...)
```

Within a `describe` group, use the `it` macro to create a single test
example. Start your example with a documentation string describing
what should happen, followed by an expression to test what you think
should be logically true.

```clojure
(describe +-test "with integers"
  (it "computes the sum of 1 and 2"
    (= 3 (+ 1 2)))
  (it "computes the sum of 3 and 4"
    (= 7 (+ 3 4))))
```

Each `it` example may only contain *one* expression, which must return
logical true to indicate the test passed or logical false to indicate
it failed.

### Nested Test Groups

Test groups may be nested inside other groups with the `testing`
macro, which has the same syntax as `describe` but does not define a
top-level Var:

```clojure
(ns examples.readme.nested
  (:use [lazytest.describe :only (describe it testing)]))

(describe "Addition"
  (testing "of integers"
    (it "computes small sums"
      (= 3 (+ 1 2)))
    (it "computes large sums"
      (= 7000 (+ 3000 4000))))
  (testing "of floats"
    (it "computes small sums"
      (> 0.00001 (Math/abs (- 0.3 (+ 0.1 0.2)))))
    (it "computes large sums"
      (> 0.00001 (Math/abs (- 3000.0 (+ 1000.0 2000.0)))))))
```

### Arbitrary Code in an Example

You can create an example that executes arbitrary code with the
`do-it` macro. Wrap each assertion expression in the
`lazytest.expect/expect` macro.

```clojure
(ns examples.readme.do-it
  (:use [lazytest.describe :only (describe do-it)]
        [lazytest.expect :only (expect)]))

(describe "Arithmetic"
  (do-it "after printing"
    (expect (= 4 (+ 2 2)))
    (println "Hello, World!")
    (expect (= -1 (- 4 5)))))
```

The `expect` macro is like `assert` but carries more information about
the failure. It throws an exception if the expression does not
evaluate to logical true.

If the code inside the `do-it` macro runs to completion without
throwing an exception, the test example is considered to have passed.

## Focusing on Individual Tests and Suites

The `describe`, `testing`, `it`, and `do-it` macros all take an
optional metadata map immediately after the docstring.

Adding `:focus true` to this map will cause *only* that test/suite to
be run. Removing it will return to the normal behavior (run all
tests).

```clojure
(describe my-test
  "fancy test"
  {:focus true}
  ...)
```

## Lazytest Internals

The smallest unit of testing is a *test case*, which is a function
(see `lazytest.test-case/test-case`). When the function is called, it
may throw an exception to indicate failure. If it does not throw an
exception, it is assumed to have passed. The return value of a test
case is always ignored. Running a test case may have side effects.
The macros `lazytest.describe/it` and `lazytest.describe/do-it` create
test cases.

Tests cases are organized into *suites*. A test suite is a function
(see `lazytest.suite/suite`) that returns a *test sequence*. A test
sequence (see `lazytest.suite/test-seq`) is a sequence, possibly lazy,
of test cases and/or test suites. Suites, therefore, may be nested
inside other suites, but nothing may be nested inside a test case.
The macros `lazytest.describe/describe` and
`lazytest.describe/testing` create test suites.

A test suite function may NOT have side effects; it is only used to
generate test cases and/or other test suites.

A test *runnner* is responsible for expanding suites (see
`lazytest.suite/expand-suite`) and running test cases (see
`lazytest.test-case/try-test-case`). It may also provide feedback on
the success of tests as they run. Two built-in runners are provided,
see `lazytest.runner.console/run-tests` and
`lazytest.runner.debug/run-tests`.

The test runner also returns a sequence of *results*, which are either
*suite results* (see `lazytest.suite/suite-result`) or *test case
results* (see `lazytest.test-case/test-case-result`). That sequence
of results is passed to a *reporter*, which formats results for
display to the user. One example reporter is provided, see
`lazytest.report.nested/report`.

## Making Emacs Indent Tests Properly

Put the following in `.emacs`:

```elisp
(eval-after-load 'clojure-mode
  '(define-clojure-indent
     (describe 'defun)
     (testing 'defun)
     (given 'defun)
     (using 'defun)
     (with 'defun)
     (it 'defun)
     (do-it 'defun)))
```

## License

Originally by [Alessandra Sierra](https://www.lambdasierra.com).

Currently developed by [Noah Bogart](https://github.com/NoahTheDuke).

Licensed under [Eclipse Public License 1.0](https://www.eclipse.org/org/documents/epl-v10.html)
