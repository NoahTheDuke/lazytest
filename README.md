# Lazytest: A new test framework for Clojure

An alternative to `clojure.test`, aiming to be feature-rich and easily
extensible.

## Why a new test framework?

> Isn't `clojure.test` good enough?

Lazytest is designed to be a standalone test system for Clojure,
disconnected from the built-in `clojure.test`. `clojure.test` has
existed since 1.1 and while it's both ubiquitous and useful, it has
a number of
[problems](https://stuartsierra.com/2010/07/05/lazytest-status-report).

Other alternatives such as `Midje` and `expectations` have attempted
to correct some of those issues and they made good progress, but many
(such as `Midje`) relied on non-list style (`test => expected`) and
most don't worked well with modern repl-driven development practices
(using `gynsym`s instead of named test vars).

I like the ideas put forth in Alessandra's post above about Lazytest
and hope to experiment with achieving them 10 years later, while
borrowing heavily from the work in both the Clojure community and test
runners frameworks in other languages.

## Getting Started

Add it to your deps.edn or project.clj:

```clojure
{:aliases
 {:test {:extra-deps {io.github.noahtheduke/lazytest {:mvn/version "0.2.0"}}
         :extra-paths ["test"]
         :main-opts ["-m" "lazytest.main"]}}}
```

In a test file:

```clojure
(ns example.readme-test
  (:require [lazytest.core :refer [defdescribe decribe expect it]]))

(defdescribe seq-fns-test
  (describe keep
    (it "should reject nils"
      (expect (= '(1 false 2 3) (keep identity [nil 1 false 2 3]))))))
```

From the command line:

```bash
$ clojure -M:test

  example.readme-test
    seq-fns-test
      #'clojure.core/keep
        √ should reject nils

Ran 1 test cases in 0.00243 seconds.
0 failures.
```

## Testing with 'lazytest'

The primary api is found in `lazytest.core` namespace. It mimics the
behavior-driven testing style popularized by libraries such as
[RSpec](https://rspec.info/) and [Mocha](https://mochajs.org).

Use the `defdescribe` macro to create a group of tests. Start the
group with a name and an optional documentation string.

```clojure
(ns examples.readme.groups
  (:require [lazytest.core :refer [defdescribe describe expect it]]))

(defdescribe seq-fns-test "a test of clojure core fns" ...)
```

Within a `defdescribe` group, use `it` to create a test example. Start
your example with a documentation string describing what should
happen, followed by an expression that throws if it it fails, such as
Clojure's built-in `assert` or Lazytest's provided `expect`.

```clojure
(defdescribe +-test "with integers"
  (it "computes the sum of 1 and 2"
    (expect (= 3 (+ 1 2))))
  (it "computes the sum of 3 and 4"
    (assert (= 7 (+ 3 4)))))
```

The `expect` macro is like `assert` but carries more information about
the failure. It throws an exception if the expression does not
evaluate to logical true.

If the code inside the `it` macro runs to completion without throwing
an exception, the test example is considered to have passed.

### Nested Test Groups

Test groups may be nested inside other groups with `describe`, which
has the same syntax as `defdescribe` but does not define a top-level
Var (thus the similar names).

```clojure
(ns examples.readme.nested
  (:require [lazytest.core :refer [defdescribe describe expect it]]))

(defdescribe addition-test "Addition"
  (describe "of integers"
    (it "computes small sums"
      (expect (= 3 (+ 1 2))))
    (it "computes large sums"
      (expect (= 7000 (+ 3000 4000)))))
  (describe "of floats"
    (it "computes small sums"
      (expect (> 0.00001 (abs (- 0.3 (+ 0.1 0.2))))))
    (it "computes large sums"
      (expect (> 0.00001 (abs (- 3000.0 (+ 1000.0 2000.0))))))))
```

## Focusing on Individual Tests and Suites

The `defdescribe`, `describe`, `expect-it`, and `it` macros all take
an optional metadata map immediately after the docstring.

Adding `:focus true` to this map will cause *only* that test/suite to
be run. Removing it will return to the normal behavior (run all
tests).

```clojure
(defdescribe my-test
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

NOTE: The macros `lazytest.describe/it` and
`lazytest.describe/expect-it` create test cases.

Tests cases are organized into *suites*. A test suite is a function
(see `lazytest.suite/suite`) that returns a *test sequence*. A test
sequence (see `lazytest.suite/test-seq`) is a sequence, possibly lazy,
of test cases and/or test suites. Suites, therefore, may be nested
inside other suites, but nothing may be nested inside a test case.

NOTE: The macros `lazytest.describe/defdescribe` and
`lazytest.describe/describe` create test suites.

A test suite function may NOT have side effects; it is only used to
generate test cases and/or other test suites.

A test *runnner* is responsible for expanding suites (see
`lazytest.suite/expand-suite`) and running test cases (see
`lazytest.test-case/try-test-case`). It may also provide feedback on
the success of tests as they run.

The test runner also returns a sequence of *results*, which are either
*suite results* (see `lazytest.suite/suite-result`) or *test case
results* (see `lazytest.test-case/test-case-result`). That sequence of
results is passed to a *reporter*, which formats results for display
to the user. Multiple reporters are provided, see the namespace
`lazytest.reporters`.

## Making Emacs Indent Tests Properly

Put the following in `.emacs`:

```elisp
(eval-after-load 'clojure-mode
  '(define-clojure-indent
     (defdescribe 'defun)
     (describe 'defun)
     (given 'defun)
     (expect-it 'defun)
     (it 'defun)))
```

## License

Originally by [Alessandra Sierra](https://www.lambdasierra.com).

Currently developed by [Noah Bogart](https://github.com/NoahTheDuke).

Licensed under [Eclipse Public License 1.0](https://www.eclipse.org/org/documents/epl-v10.html)
