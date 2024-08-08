# Change Log

## Unreleased

### Added

- `-n`, `--namespace NS` cli flag to specify namespaces that will be tested.
- `-v`, `--var VAR` cli flag to specify vars that will be tested.

Note: If both `--namespace` and `--var` are provided, then Lazytest will run all tests within the namespaces AND the specified vars. They are inclusive, not exclusive.

## 0.3.0 - 2024-08-08

- Added "Usage" to README, listing cli options.
- Print correct version in `--help` output.
- Add `--exclude` and `--include` cli flags for metadata selection. `--include` works like `^:focus` but is arbitrary metadata.

## v0.2.1 - 2024-06-26 

- Cleaned up README, added Editor Integration section.

## v0.2.0 - 2024-06-18

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
  * `debug` prints "Running X" and "Done with X" for all test sequences and print the direct result of all test cases.
- Add tests for reporters.

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

## Old Lazytest

This file documents the original Alessandra Sierra development, which ended in 2013. It is kept for posterity.

v2.0.0-SNAPSHOT

 * Remove contexts and `given`

 * Added deftest style

 * Added sample projects for Leiningen & Maven

 * Added samples using clojure.test-clojure


v1.2.3        2010-11-27        Added Maven Plugin

 * Separated into modules

 * Released lazytest-maven-plugin version 1.0.0


v1.2.2        2010-11-27        Fixed release with diff

 * Fixed release using 'diff' and Clojure 1.3.0-alpha3


v1.2.1        2010-11-27        BAD RELEASE: DO NOT USE

 * Wrong file extension on the deployed JAR

 * Tried maven-release-plugin version 2.1

 
v1.2.0        2010-11-27        BAD RELEASE: DO NOT USE

 * Wrong file extension on the deployed JAR

 * Depends on Clojure 1.3.0-alpha3

 * Prints 'diff' of values when testing with =


v1.1.2        2010-10-03        Minor reloading enhancement

 * Only touch source files on reloading when absolutely necessary


v1.1.1        2010-10-02        Bugfix release

 * Prevent 'namespace not found' bug by touching source files before
   reloading


v1.1.0        2010-10-02        Improvements for random generators

 Note: due to a configuration error, this version was never deployed.

 * Add lazytest.random/default-test-case-count and
   scaled-test-case-count

 * lazytest.describe/for-any uses the above

 * lazytest.report.nested collapses large groups of test cases with
   the same doc strings, such as randomly-generated tests


v1.0.2        2010-10-01        Bugfix release

 * Fix parsing of ns forms with nested prefix lists


v1.0.1        2010-09-29        Bugfix release

 * Fix missing :require of lazytest.random within lazytest.describe

 * Remove libs from clojure.core/*loaded-libs* when reloading. This is
   manipulating an undocumented core Var, but avoids some unnecessary
   loads caused by (require :reload-all ...). It also seems to avoid
   some load-order issues with `:reload-all`, but I can't find
   consistent test cases.


v1.0.0        2010-09-24        Initial Release
