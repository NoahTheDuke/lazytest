# Lazytest API

In short: Tests are defined with `defdescribe`, suites (groups of nested suites and test cases) are defined with `describe`, test cases are defined with `it`, and assertions are defined with `expect` but can be any function that throws an exception on failure. When `lazytest.main/run` is called, all loaded namespaces are checked for tests, and the contained test cases are run (with suites providing context and additional documentation).

Hooks can be used in `:context` metadata on suites, and assertion helpers can be used in test cases.

```clojure
(require '[lazytest.core :refer :all])
(import 'lazytest.ExpectationFailed)
```

## `expect`

Create an assertion. Throws if expression returns logical false. The optional message is used as the exception message.

```clojure
(expect (= 1 1))
=> true

(try (expect nil "What do you expect?")
  (catch ExpectationFailed t (ex-message t)))
=> "What do you expect?"
```

## `it`

Creates a test case, which runs arbitrary code. If the `:body` code throws, it's considered a failing test. Otherwise, it's considered a passing test.

```clojure
(it "returns a test-case object" (expect (= 1 1)))
=> lazytest.test-case/test-case?

(:body (it "stores the body in :body" (expect (= 1 1))))
=> fn?

((:body (it "stores the body in :body" (expect (= 1 1)))))
=> true

(:doc (it "stores the body in :body" (expect (= 1 0))))
=> "stores the body in :body"
```

## `expect-it`

Combination of `expect` and `it` to assert a single expression. Useful when the expression has no set up or multiple steps.

```clojure
(expect-it "returns a test-case object" (= 1 1))
=> lazytest.test-case/test-case?

((:body (expect-it "works like `it`" (= 1 1))))
=> true
```

## `describe`

Creates a test suite, which is a collection of test cases or other test suites.

```clojure
(describe "A suite of test cases or other suites"
  (it "can hold test cases" (expect (= 1 1)))
  (describe "and nested suites"
    (it "with other test cases" (expect (= 1 1)))))
=> lazytest.suite/suite?

(:children (describe "sub suites are under :children" nil))
=> sequential?
```

## `defdescribe`

Defines a var binding a function that contains a test suite.

```clojure
(defdescribe temp-test
  (it "is a test" (expect (= 1 1))))

temp-test
=> fn?

(temp-test)
=> lazytest.suite/suite?
```

# Hooks

## `before` & `after`

Runs arbitrary code once before (or after) all nested suites and test cases. They bind the body in an anonymous function, held in a map:

```clojure
(before (prn "This is happening!"))
=> map?

(keys (before (prn "This is happening!")))
=> [:before]

(after (prn "This is happening!"))
=> map?

(keys (after (prn "This is happening!")))
=> [:after]
```

# Helpers

## `throws?`

Calls given no-arg function, returns true if it throws expected class, rethrows other Throwables.

```clojure
(throws? clojure.lang.ExceptionInfo (constantly :no-op))
=> false

(try (throws? AssertionError #(throw (ex-info "yikes" {})))
  (catch clojure.lang.ExceptionInfo ex false))
=> false

(throws? clojure.lang.ExceptionInfo #(throw (ex-info "yikes" {})))
=> true
```

## `throws-with-msg?`

Calls given function with no arguments, returns true if it throws the expected class and the message matches given regex, throws an ExpectationFailed if the class matches but the regex fails, and rethrows other throwables.

```clojure
(throws-with-msg? clojure.lang.ExceptionInfo #"yikes" #(do :no-op))
=> false

(try (throws-with-msg? AssertionError #"yikes" #(throw (ex-info "yikes" {})))
  (catch clojure.lang.ExceptionInfo ex :caught))
=> :caught

(try (throws-with-msg? clojure.lang.ExceptionInfo #"foo" #(throw (ex-info "yikes" {})))
  (catch ExpectationFailed ex :caught))
=> :caught

(throws-with-msg? clojure.lang.ExceptionInfo #"yikes" #(throw (ex-info "yikes" {})))
=> true
```

## `causes?`

Calls given no-arg function, returns true if it throws expected class anywhere in the cause chain.

```clojure
(causes? clojure.lang.ExceptionInfo #(do :no-op))
=> false

(try (causes? AssertionError #(throw (ex-info "yikes" {})))
  (catch clojure.lang.ExceptionInfo ex :caught))
=> :caught

(causes? clojure.lang.ExceptionInfo
  #(throw (ex-info "yikes" {})))
=> true

(causes? IllegalArgumentException
  #(throw (ex-info "yikes" {} (IllegalArgumentException. "inner ex"))))
=> true
```

* `causes-with-msg?`: Calls given no-arg function, returns true if it throws expected class anywhere in the cause chain and the cause's message matches given regex.

## `ok?`

Calls given no-arg function, returns true if no exception is thrown. (Useful for ignoring logical false return values.)

```clojure
(ok? (constantly false))
=> true

(ok? (constantly nil))
=> true
```
