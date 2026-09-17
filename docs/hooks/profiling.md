# Hook: Profiling

Print the slowest namespaces and test vars by duration. By default, it will print 5 namespaces and 5 vars, but this can be changed with `--profiling-count`. Can be disabled with `--no-profiling`.

```
$ clojure -M:dev:test:lazytest --hook profiling

Top 5 slowest test namespaces (0.48764 seconds, 45.9% of total time)
  lazytest.libs-test 0.40812 seconds
  lazytest.main-test 0.03445 seconds
  lazytest.find-test 0.01646 seconds
  lazytest.reporters-test 0.01585 seconds
  lazytest.core-test 0.01277 seconds

Top 5 slowest test vars (0.47278 seconds, 44.5% of total time)
  lazytest.libs-test/honeysql-test 0.40808 seconds
  lazytest.main-test/filter-ns-test 0.03442 seconds
  lazytest.find-test/find-var-test-value-test 0.01643 seconds
  lazytest.reporters-test/results-test 0.00791 seconds
  lazytest.core-test/expect-helpers-test 0.00594 seconds
```
