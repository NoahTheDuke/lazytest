# Extension: Matcher Combinators

Wrapping macros to ease use of [nubank/matcher-combinators](https://github.com/nubank/matcher-combinators/) when writing Lazytest assertions.

## deps.edn

Add `nubank/matcher-combinators` to your test alias alongside `lazytest`:

```clojure lazytest/skip=true
{:aliases
 {:test {:extra-deps {io.github.noahtheduke/lazytest {:mvn/version "2.1.0"}
                      nubank/matcher-combinators {:mvn/version "3.11.0"}}
         ...}}}
```

## Usage

The `lazytest.extensions.matcher-combinators` namespace only defines two macros, [`match?`][match] and [`thrown-match?`][thrown]. Everything else from `matcher-combinators` can be used directly.

* [`match?`][match]: Asserts that `actual` matches `expected`, where `expected` can be a value, predicate function, or concrete Matcher. If they don't match, throws a `ExpectationFailed`, like [`lazytest.core/expect`][expect].
* [`thrown-match?`][thrown]: Runs the given expr, and asserts that it throws an exception (defaults to `clojure.lang.ExceptionInfo`) and that the [`ex-data`][ex-data] matches an `expected` value (which can be a value, predicate function, or concrete Matcher).

For more examples and better documentation, please see matcher-combinators' own [documentation][mc].

## Example

```clojure
(ns lazytest.matcher-combinators.example-test
  (:require
    [lazytest.core :refer [defdescribe describe expect it]]
    [lazytest.extensions.matcher-combinators :refer [match? thrown-match?]]
    [lazytest.expectation-failed :refer [ex-failed?]]))

(defdescribe matchers-test
  (describe "correctly mirrors the assert-expr functionality"
    (it match?
      (expect (match? {:foo 1} {:foo 1 :bar 2})))
    (it thrown-match?
      (expect (thrown-match? {:foo 1}
                (throw (ex-info "heck" {:foo 1})))))))
```

[ex-data]: https://clojuredocs.org/clojure.core/ex-data
[expect]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#expect
[match]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.extensions.matcher-combinators#match?
[mc]: https://github.com/nubank/matcher-combinators/
[thrown]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.extensions.matcher-combinators#thrown-match?
