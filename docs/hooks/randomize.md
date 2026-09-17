# Hook: Randomize

By default, Lazytest will run all tests in the order they're found, which adheres pretty closely to a depth-first traversal of the test directory. This isn't always desireable, so to help combat that, the hook `lazytest.hooks/randomize` has been written to allow you to randomize the order.

When included, the hook will randomize all namespaces, all test vars with namespaces, and then all nested suites and test-cases. This does not shuffle test vars between namespaces nor does it move nested suites or test cases around; this is merely a re-ordering of each child object.

The granularity can be changed with `--randomize`: `all` for everything (default), `ns` for only shuffling namespapces, `var` for only shuffling test vars, `suites` for only shuffling the suites within test vars, and `none` to disable.

Likewise, the shuffle is done with a random seed which is printed at the end of the run. The same ordering can be achieved by passing `--randomize-seed` with a previous run's seed.

```
$ clojure -M:dev:test:lazytest --hook randomize

lazytest.order-test
  One
    √ 1 equals one
  Four
    √ 4 equals four
    √ 1 equals one
    √ 5 equals five
    √ 3 equals three
    √ 2 equals two
  Two
    √ 2 equals two
  Three
    √ 3 equals three

Ran with --seed 263867813
```
