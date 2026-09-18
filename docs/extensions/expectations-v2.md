# Extension: Expectations v2-stye API

Adapts the `expect` assertion and utility functions from [Expectations v2](https://github.com/clojure-expectations/clojure-test). The interface vars (`defexpect`, `expecting`, etc) have also been adapted. Due to the differences in Lazytest and `clojure.test`, test cases must be defined with [`lazytest.core/it`][it], as [`expect`][expect-v2] is merely an assertion.

Because all vars have been adapted, no additional dependencies are required.

See [namespace docs][namespace docs] for further details.

## Example

```clojure
(ns lazytest.expectations-v2.example-test
  (:require
    [lazytest.core :refer [it]]
    [lazytest.extensions.expectations :refer [defexpect expecting expect]]))

(defexpect example-test
  (expecting "many ways to work"
    (it "is a cool assertion DSL"
      (expect 2 2))))
```

[namespace docs]:  https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.extensions.expectations
[expect-v2]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.extensions.expectations#expect
[it]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#it
