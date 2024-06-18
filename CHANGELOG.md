# Change Log

## Unreleased

- Rename test-case-result keys to match clojure.test and other test language's runners:
  * `:form` -> `:expected`
  * `:result` -> `:actual`
- Collapse `:fail` and `:error` into `:fail`. If reporters want to differentiate, they can by checking if `:thrown` is an `ExpectationFailed`. (See `clojure-test` reporter for an example.)
- Add `(message, reason)` constructor arity to ExpectationFailed to better match both `AssertionError` and `ExceptionInfo`.
- Catch ExpectationFailed in `expect`, rethrow with updated `:message`, instead of passing `msg` into each assert-expr like in clojure.test.
- Catch other Throwables in `expect`, wrap in ExpectationFailed as `:caught` data.
- Simplify how `:message`s are tracked.
- Require a docstring expression from `describe`, `it` and `expect-it`.
- Add `*color*` dynamic var (set to lazytest.colorize system env, default to true) and make `colorize?` rely on it.
- Move all relevant test case information into result object.
- Move all relevant suite information into result object.
- Filter suites/test cases when running a single var.
- Add support for [nubank/matcher-combinators](https://github.com/nubank/matcher-combinators).
- Sort test vars by line and column before running.
- Rewrite runner to use type-based multimethod with report hooks.
- Rewrite reporters to use new report hooks, allow for multiple hooks to be combined:
  * `focused` prints if there's any focused tests.
  * `summary` prints "Ran x test cases, N failures, Y errors".
  * `results` prints failed and errored test cases, expected values, etc.
  * `dots` prints `.` for passing test case, and `F` for failure. Namespaces wrap test cases in parentheses: `(..F.)` Includes `focused`, `results`, and `summary`.
  * `nested` prints each suite and test case on a new line, and indents each suite. Includes `focused`, `results`, and `summary`.
  * `clojure-test` attempts to mimic clojure.test's basic output.
  * `verbose` prints "Running X" and "Done with X" for all test sequences and print the direct result of all test cases.
- Add tests for reporters.
- Add `--verbose` cli flag which prepends the `verbose` reporter to any other reporters.

## 0.1.0 - 2024-06-09

Updated original code to use deps.edn, tools.build, and other modern tooling.

### Big changes

* Remove all maven-specific and leiningen-specific code.
* Remove `clojure.test`-like api.
* Remove random sampling code.
* Remove dependency-tracking and reloading and autotest code.
* Remove clojure 1.3 test code examples.
* Add CLI api.
* Change external API to use a single namespace (`lazytest.core`).
* Change external API macros and functions to work better with modern tooling (use vars, etc).
