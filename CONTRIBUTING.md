# Contributing

If you want to contribute to Lazytest, I am eternally grateful. This is a labor of love and it's invigorating to have others help out.

* Issues are always welcome, a good way to help discover edge cases and incorrect impls.
* Bug fixes and small touch ups can be put in a PR, no problem. (Documentation updates are also fair game.)
* For anything bigger, I prefer to have an issue first so we can discuss the scope and nature of the bug, feature, or idea you have.

## Notes on tests

As this is a test runner, it's expected that you use lazytest to write all tests herein.

#### Matching exceptions

If you wish to match against a test case failure, you can use `(ex-info "message" {...data here...})` in a `match?` call. I've extended the `mc/Matchers` protocol for `ExceptionInfo`, and done some light clean-up so we can get nice output when there's mismatchs. Additionally, you can specify `:type` in the ex-info map, to set the expected exception type.

(See [test/clojure/lazytest/test_utils.clj] for the details.)
