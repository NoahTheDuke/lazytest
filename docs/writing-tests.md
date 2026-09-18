# Writing tests with Lazytest

The primary api is found in `lazytest.core` namespace. It mimics the behavior-driven testing style popularized by libraries such as [RSpec](https://rspec.info/) and [Mocha](https://mochajs.org).

Define tests with [`defdescribe`][defdescribe], group test suites and test cases together into a suite with [`describe`][describe], and define test cases with [`it`][it]. [`describe`][describe] can be nested. [`defdescribe`][defdescribe]'s docstring is optional, [`describe`][describe] and [`it`][it]'s docstrings are not.

```clojure
(require '[lazytest.core :refer [defdescribe describe expect it]])

(defdescribe +-test "with integers"
  (it "computes the sum of 1 and 2"
    (expect (= 3 (+ 1 2))))
  (it "computes the sum of 3 and 4"
    (assert (= 7 (+ 3 4))))
  (describe "associative property"
    (it "works in both directions"
      (expect (= 3 (+ 1 2)))
      (expect (= 3 (+ 2 1))))))
```

The [`expect`][expect] macro is like [`assert`][assert] but carries more information about the failure, such as the given form, the returned value, and the location of the call. It throws an exception if the expression does not evaluate to logical true.

If an [`it`][it] runs to completion without throwing, the test case is considered to have passed.

The [`describe`][it] macro creates a test suite, a map containing `:children` (among other things) which are nested suites or test cases (created with [`it`][it]). It can be passed to other functions or tests, it can be updated with other code, it can be removed from parent suites. Unlike [`clojure.test/testing`][testing], it is not merely setting a context string that is used to generate helpful error messages.

> [!IMPORTANT]
> This is maybe the greatest divergence from [`clojure.test`](https://clojuredocs.org/clojure.test), so it's important to emphasize this. Test cases (the objects created by [`it`][it]) are **not run** when a test function ([`defdescribe`][defdescribe]) is called or a test suite ([`describe`][describe]) is evaluated. Each of these returns an object (a map, to be specific), and the [`lazytest.runner`][lazytest.runner] machinery traverses them and calls the test case function body only when appropriate. This means that you cannot write normal clojure code outside of [`it`][it] blocks, as it will work slightly differently than anticipated.
>
> For more details and ways to work around this, please read the section on [Setup and Teardown](docs/writing-tests/setup-teardown.md).

## Aliases

To help write meaningful tests, a couple aliases have been defined for those who prefer different vocabulary:

* [`context`][context] for [`describe`][describe] (this is discouraged because it clashes with the `:context` block, but it's retained for consistency).
* [`specify`][specify] for [`it`][it]
* [`should`][should] for [`expect`][expect]

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

## Var Metadata

In addition to finding the tests defined with [`defdescribe`][defdescribe], Lazytest also checks all vars for `:lazytest/test` metadata. If the `:lazytest/test` metadata is a function, a test case, or a test suite, it's treated as a top-level [`defdescribe`][defdescribe] for the associated var and executed just like other tests. `:lazytest/test` functions are given the doc string ``"`:lazytest/test` metadata"``.

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

[assert]: https://clojuredocs.org/clojure.core/assert
[context]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#context
[defdescribe]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#defdescribe
[describe]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#describe
[expect]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#expect
[it]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#it
[lazytest.clojure-test]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.experimental.interfaces.clojure-test
[lazytest.midje]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.experimental.interfaces.midje
[lazytest.qunit]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.experimental.interfaces.qunit
[lazytest.runner]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.runner
[lazytest.xunit]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.experimental.interfaces.xunit
[should]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#should
[specify]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#specify
[testing]: https://clojuredocs.org/clojure.test/testing
