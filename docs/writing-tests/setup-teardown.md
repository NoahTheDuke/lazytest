# Setup and Teardown

To handle set up and tear down of stateful architecture, Lazytest provides the context macros [`before`][before], [`before-each`][before-each], [`after-each`][after-each], [`after`][after], [`around`][around], and [`around-each`][around-each], along with the helper function [`set-ns-context!`][set-ns-context]. You can call them directly in a [`describe`][describe] block or add them to a `:context` vector in suite metadata, or you can write the function directly as a map with the macro names as keywords. (To read a more specific description of how this works, please read [Run Lifecycle Overview](docs/deep-dives/run-lifecycle-overview.md).

<!-- toc -->

- [High Level Example](#high-level-example)
- [Context functions run in two directions](#context-functions-run-in-two-directions)
- [Context functions work in two different modes](#context-functions-work-in-two-different-modes)
  * [Context functions that run once](#context-functions-that-run-once)
  * [Context functions that run multiple times](#context-functions-that-run-multiple-times)
- [Namespace-level context functions](#namespace-level-context-functions)
- [Common Patterns](#common-patterns)
  * [`with-redefs` to stub a logger](#with-redefs-to-stub-a-logger)
  * [`use-fixtures :each` to reset a dynamic variable for every test](#use-fixtures-each-to-reset-a-dynamic-variable-for-every-test)
  * [`use-fixtures :once` to share a database connection across all tests](#use-fixtures-once-to-share-a-database-connection-across-all-tests)
  * [Generating data to be used across a whole test](#generating-data-to-be-used-across-a-whole-test)

<!-- tocstop -->

## High Level Example

```clojure
(require '[lazytest.core :refer [expect-it before before-each after-each after around]])

(defdescribe before-and-after-test
  (let [state (volatile! [])]
    (describe "before and after example"
      (before (vswap! state conj :before))
      (after (vswap! state conj :after))
      (expect-it "can do side effects" (vswap! state conj :expect)))
    (describe "results"
      (expect-it "has been properly tracked"
        (= [:before :expect :after] @state)))))

(defdescribe around-test
  (let [state (volatile! [])]
    (describe "around example"
      {:context [(around [f]
                   (vswap! state conj :around-before)
                   (f)
                   (vswap! state conj :around-after))]}
      (expect-it "can do side effects" (vswap! state conj :expect)))
    (describe "results"
      (expect-it "correctly ran the whole thing"
        (= [:around-before :expect :around-after] @state)))))

(defdescribe each-test
  (let [state (volatile! [])]
    (describe "each examples"
      {:context [{:before (fn [] (vswap! state conj :before))
                  :before-each (fn [] (vswap! state conj :before-each))}]}
      (expect-it "can do side effects" (vswap! state conj :expect-1))
      (expect-it "can do side effects" (vswap! state conj :expect-2)))
    (expect-it "has been properly tracked"
      (= [:before :before-each :expect-1 :before-each :expect-2] @state))))
```

## Context functions run in two directions

Every [`around`][around], [`before`][before], [`around-each`][around-each], and [`before-each`][before-each] function is called in the order its defined, and every [`after`][after] and [`after-each`][after-each] is called in the opposite order it's defined. This is because [`before`][before]/[`before-each`][before-each] and [`after`][after]/[`after-each`][after-each] are intended to act like one half of an [`around`][around] or [`around-each`][around-each] call, which are designed like [`clojure.core/with-open`][with-open] and other similar functions. So [`before`][before] is called forward, and then [`after`][after] is called backward. This can be confusing, but I promise it's worthwhile.

## Context functions work in two different modes

### Context functions that run once

The [`around`][around]/[`before`][before]/[`after`][after] context functions are run only by the suite or test case they're defined for. So a [`before`][before] at the top of a test var will be run once before any child is evaluated, and a nested [`around`][around] will only wrap the evaluation of any children suites or test-cases, not parent or sibling suites. The same is true for test-cases: Any [`around`][around]/[`before`][before]/[`after`][after] context functions will be evaluated only once for that specific test-case.

### Context functions that run multiple times

The [`around-each`][around-each]/[`before-each`][before-each]/[`after-each`][after-each] context functions are not run for the suite they're defined for, they're run by every nested test case, no matter how nested. A given test case gathers _all_ parent `*-each` context functions, and then executes them with [`around-each`][around-each] wrapping any [`before-each`][before-each] or [`after-each`][after-each] functions. As with [`after`][after], [`after-each`][after-each] is evaluated in reverse declaration order.

## Namespace-level context functions

To set context functions for an entire namespace, use `set-ns-context!`. There is currently no way to define run-wide context functions.

In `clojure.test`, `(use-fixtures :each ...)` will set the provided fixtures to wrap each test var. To achieve the same in Lazytest, define a var of the target context function and add it to the [`defdescribe`][defdescribe]'s `:context` block of each var in the namespace. This is necessarily more tedious than [`use-fixtures`][use-fixtures], but it is also more explicit and gracefully handles special cases (define multiple functions to handle subtle differences, use whichever is situationally helpful).

```clojure lazytest/skip=true
(defonce ^:dynamic *db-connection* nil)
(def prep-db
  (around [f]
    (binding [*db-connection* (get-db-connection ...)]
      (f))))

(defdescribe needs-a-db-test
  {:context [prep-db]}
  (it "has the right connection"
    (expect (= 1 (count (sql/query *db-connection* "SELECT * FROM users;"))))))
```

> [!IMPORTANT]
> To repeat myself, test cases (the objects created by [`it`][it]) are **not run** when a test function ([`defdescribe`][defdescribe]) is called or a test suite ([`describe`][describe]) is evaluated. Each of these returns an object (a map, to be specific), and the `lazytest.runner` machinery traverses them and calls the test case function body only when appropriate. This means that you cannot write normal clojure code outside of [`it`][it] blocks, as it will work slightly differently than anticipated.
>
> If you want to create data that will be used by multiple test cases or test suites, I would recommend against merely `let`-binding it as it will be bound when the test suite is created, not when the test cases are executed (unless it's a bit of literal data, aka a number, a set, etc). Any variable that relies on a function call should either be wrapped in a `delay` (to prevent execution until within the context of a test case), or set to a `volatile` or `atom` and then assigned in a [`before`][before] block.
>
> If you want to use something like [`with-redefs`][with-redefs] or [`with-open`][with-open] or [`with-bindings`][with-bindings] (macros that change a value only during the execution of a body), you _must_ put them into an [`around`][around] context block. Otherwise, the state they temporarily set will only exist during the evaluation/creation of the test suite, and will not exist when the test case is executed.

## Common Patterns

To make this very clear, here are some patterns I've seen in `clojure.test` test suites, and how they might look in Lazytest.

### [`with-redefs`][with-redefs] to stub a logger

```clojure lazytest/skip=true
(ns cool.example-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [clojure.tools.logging :as log]
    [cool.example :as c.e]))

(deftest cool-func-test
  (with-redefs [log/log* (constantly nil)]
    (testing "with keywords"
      (is (c.e/cool-func :a :b :c)))
    (testing "with strings"
      (is (c.e/cool-func "a" "b" "c")))))
```

```clojure lazytest/skip=true
(ns cool.example-test
  (:require
    [lazytest.core :refer [defdescribe describe around expect-it]]
    [clojure.tools.logging :as log]
    [cool.example :as c.e]))

(defdescribe cool-func-test
  (around [f]
    (with-redefs [log/log* (constantly nil)]
      (f))
  (describe "with keywords"
    (expect-it "works" (c.e/cool-func :a :b :c)))
  (describe "with strings"
    (expect-it "works" (c.e/cool-func "a" "b" "c")))))
```

### `use-fixtures :each` to reset a dynamic variable for every test

```clojure lazytest/skip=true
(ns cool.example-test
  (:require
    [clojure.test :refer [deftest testing is use-fixtures]]
    [com.stuartsierra.component :as component]
    [cool.example :as c.e]))

(defn set-state-fn [f]
  (binding [c.e/*state* (component/start (c.e/new-system))]
    (f)))

(use-fixtures :each #'set-state-fn)

(deftest system-func-test
  (testing "example 1"
    (is (c.e/system-func *state* :foo :bar))))

(deftest system-func-2-test
  (testing "example 2"
    (is (c.e/system-func-2 *state* :foo :bar))))
```

```clojure lazytest/skip=true
(ns cool.example-test
  (:require
    [lazytest.core :refer [defdescribe describe around expect-it]]
    [com.stuartsierra.component :as component]
    [cool.example :as c.e]))

(def set-state-fn
  (around [f]
    (binding [c.e/*state* (component/start (c.e/new-system))]
      (f))))

(defdescribe system-func-test
  {:context [set-state-fn]}
  (describe "example 1"
    (expect-it "works" (c.e/system-func *state* :foo :bar))))

(defdescribe system-func-2-test
  {:context [set-state-fn]}
  (describe "example 2"
    (expect-it "works" (c.e/system-func-2 *state* :foo :bar))))
```

### `use-fixtures :once` to share a database connection across all tests

```clojure lazytest/skip=true
(ns cool.example-test
  (:require
    [clojure.test :refer [deftest testing is use-fixtures]]
    [next.jdbc :as jdbc]
    [cool.example :as c.e]))

(defn set-db-connection [f]
  (with-open [c.e/*connection* (jdbc/get-connection c.e/datasource)
    (f)))

(use-fixtures :once #'set-db-connection)

(deftest system-func-test
  (testing "example 1"
    (is (c.e/system-func c.e/*connection* :foo :bar))))

(deftest system-func-2-test
  (testing "example 2"
    (is (c.e/system-func-2 c.e/*connection* :foo :bar))))
```

```clojure lazytest/skip=true
(ns cool.example-test
  (:require
    [lazytest.core :refer [defdescribe describe around expect-it set-ns-context!]]
    [next.jdbc :as jdbc]
    [cool.example :as c.e]))

(set-ns-context!
 [(around [f]
    (with-open [c.e/*connection* (jdbc/get-connection c.e/datasource)
      (f)))])

(defdescribe system-func-test
  (describe "example 1"
    (expect-it "works" (c.e/system-func c.e/*connection* :foo :bar))))

(defdescribe system-func-2-test
  (describe "example 2"
    (expect-it "works" (c.e/system-func-2 c.e/*connection* :foo :bar))))
```

### Generating data to be used across a whole test

```clojure lazytest/skip=true
(ns cool.example-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [clojure.tools.logging :as log]
    [cool.example :as c.e]))

(deftest state-func-test
  (let [state (c.e/generate-big-state)]
    (testing "with keywords"
      (is (c.e/state-func state :foo)))
    (testing "with strings"
      (is (c.e/state-func state "bar")))))
```

```clojure lazytest/skip=true
(ns cool.example-test
  (:require
    [lazytest.core :refer [defdescribe describe around expect-it]]
    [clojure.tools.logging :as log]
    [cool.example :as c.e]))

(defdescribe state-func-test
  (let [state (delay (c.e/generate-big-state))]
    (describe "with keywords"
      (expect-it "works" (c.e/state-func @state :foo)))
    (describe "with strings"
      (expect-it "works" (c.e/state-func @state "bar")))))
```

[after-each]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#after-each
[after]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#after
[around-each]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#around-each
[around]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#around
[before-each]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#before-each
[before]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#before
[describe]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#describe
[set-ns-context!]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#set-ns-context!
[with-bindings]: https://clojuredocs.org/clojure.core/with-bindings
[with-open]: https://clojuredocs.org/clojure.core/with-open
[with-redefs]: https://clojuredocs.org/clojure.core/with-redefs
