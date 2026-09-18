# Lazytest Internals

The smallest unit of testing is a *test case* (see `lazytest.test-case/test-case`). When the `:body` function is called, it may throw an exception to indicate failure. If it does not throw an exception, it is assumed to have passed. The return value of a test case is always ignored. Running a test case may have side effects.

> [!NOTE]
> The macros [`lazytest.core/it`][it] and [`lazytest.core/expect-it`][expect-it] create test cases.

Tests cases are organized into *suites* (see `lazytest.suite/suite`). A suite has `:children`, which is a sequence, possibly lazy, of test cases and/or test suites. Suites, therefore, may be nested inside other suites, but nothing may be nested inside a test case.

> [!NOTE]
> The macro [`lazytest.core/describe`][describe] creates a test suite. The macro [`lazytest.core/defdescribe`][defdescribe] creates a no-argument function that returns a test suite.

A test suite body SHOULD NOT have side effects; it is only used to generate test cases and/or other test suites.

The test *runner* is responsible for gathering suites (see [`lazytest.find/find-suite`][find-suite] and [`lazytest.filter/filter-tree`][filter-tree]) and running test cases (see [`lazytest.test-case/try-test-case`][try-test-case]). It may also provide feedback on the success of tests as they run.

The test runner also returns a sequence of *results*, which are either *suite results* (see [`lazytest.suite/suite-result`][suite-result]) or *test case results* (see `lazytest.test-case/test-case-result`). That sequence of results is passed to a *reporter*, which formats results for display to the user. Multiple reporters are provided, see the namespace `lazytest.reporters`.

[defdescribe]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#defdescribe
[describe]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#describe
[expect-it]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#expect-it
[filter-suite]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#lazytest.filter#filter-suite
[filter-tree]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#lazytest.filter#filter-tree
[it]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#it
[suite-result]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.suite#suite-result
[try-test-case]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.test-case#try-test-case
