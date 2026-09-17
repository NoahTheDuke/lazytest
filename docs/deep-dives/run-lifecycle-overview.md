# Run Lifecycle Overview

This is inspired by [Mocha](https://mochajs.org)'s excellent documentation.

## From the CLI

1. A user runs Lazytest, either through leiningen or Clojure CLI.
2. Lazytest parses the command line arguments to determine the relevant configuration.
3. Lazytest finds test files. If the user provides `--dir`, then every file in the file trees of all given directories are checked. Otherwise, all files within the `test` directory are checked.
4. Lazytest loads all test files. Using `tools.namespace`, the namespace of each `.clj` is extracted and `require`d, which creates the necessary vars.
5. Lazytest gathers all test vars from the required namespaces. It checks each var in each namespace against the following list of questions.
    1. Is the var defined with `defdescribe`? Call the `defdescribe`-constructed function and use the result.
    2. Does the var point to a `suite`? Resolve the var and use the result.
    3. Does the var have `:lazytest/test` metadata that is either a suite (`describe`) or a test case (`it`)? Create a new suite with `describe` and set the `:lazytest/test` metadata as a child.
    4. Does the var have `:lazytest/test` metadata that is a function? Create a new suite with `describe`, create a new test case with `it`, and then set the docstring for the test case to `:lazytest/test metadata`, and the body to calling the `:lazytest/test` metadata function.
6. Lazytest groups each namespace into a `:lazytest/ns` suite, and then groups all of the namespace suites into a `:lazytest/run` suite.
7. Lazytest does a depth-first walk of the run suite, filtering nses by `--namespace`, vars by `--var`, and all suites and test cases by `--include` or `--exclude` (with `:focus` being automatically included). These are prioritized as such:
    1. `--namespace` narrows all namespaces to those that exactly match. The namespaces of `--var` vars are included as well. If `--namespace` is not provided, all namespaces are selected.
    2. `--var` narrows all vars from the selected namespaces. If `--namespace` is provided, all vars from those namespaces are selected as well. If `--var` is not provided, all vars are selected.
    3. The suite for each var is selected by selecting all `--include` or `:focus` metadata suites and tests cases and then removing all `--exclude` suites and test cases. If no suites or test cases have `:focus` metadata or `--include` hasn't been provided, then everything is selected. (To be clear, `--exclude` overrides `:focus` and `--include`.)
8. Lazytest calls the runner on the filtered run suite.
    * For suites:
        1. If there are any `around` context functions, combine them with `clojure.test/join-fixtures`, and then execute the rest of the steps in a thunk wrapped in the combined `around` function.
        2. Run each `before` context function.
        3. For each child in `:children`, restart from step 1 of the appropriate sequence.
        4. Run each `after` context function.
    * For test cases:
        1. If there are any `around` context functions, combine them with `clojure.test/join-fixtures`, and then execute the rest of the steps in a thunk wrapped in the combined `around` function.
        2. Run each `before` context function.
        3. Run each `before-each` function (including from all parents), outermost first, in definition order.
        4. Execute the test function, get the `test-case-result`.
        5. Run each `after-each` function (including from all parents), innermost first, in definition order.
        6. Run each `after` context function.
9. Depending on the chosen reporter, Lazytest prints the results of each suite and test case immediately or at another point.
10. The run is ended with `System/exit`, and the exit value is either `0` for no failures or `1` for any number of failures.

## Programmatically

The process is roughly the same as from the CLI, but with CLI-specific steps skipped.

1. Build a suite.
    * If using `lazytest.repl/run-tests`, the specified namespace used as the required namespace.
    * If using `lazytest.repl/run-all-tests`, all currently loaded are used (found with `clojure.core/all-ns`).
    * If using `lazytest.repl/run-test-var`, the single var is used as the suite.
2. If not given a var, step 5 is executed as described above to produce a suite.
3. Steps 7-9 are executed as described above on the suite, with the note that only `:focus` is considered when filtering.
4. The results from the run are summarized and returned to the caller.
