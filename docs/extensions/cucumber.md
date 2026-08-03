# Hook: Cucumber

Quoting from the [Cucumber website](https://cucumber.io/), "Cucumber is a tool for running automated acceptance tests, written in plain language. Because they're written in plain language, they can be read by anyone on your team, improving communication, collaboration and trust."

Relying on the excellent Clojure wrapper library [lambdaisland/kaocha-cucumber](https://github.com/lambdaisland/kaocha-cucumber), the hook `lazytest.extensions.cucumber/hook` provides support for Cucumber tests.

## deps.edn

Add `lambdaisland/kaocha-cucumber` to your test alias alongside `lazytest`, excluding `lambdaisland/kaocha` to avoid dependency bloat and potential classpath clashes.

```clojure lazytest/skip=true
{:aliases
 {:test {:extra-deps {io.github.noahtheduke/lazytest {:mvn/version "2.1.0"}
                      lambdaisland/kaocha-cucumber {:mvn/version "0.11.100"
                                                    :exclusions [lambdaisland/kaocha]}}
         ...}}}
```

## Quick tutorial

Cucumber tests are behavioral tests written in a sort-of "plain language" called Gherkin that requires glue code called steps to actually convert the test steps into executable code.

Steps are defined globally in Clojure code, generally in dedicated namespaces. The namespace [[lazytest.extensions.cucumber]] defines macros that match the following Gherkin constructs: `Given`, `And`, `But`, `When`, `Then`, `Before`, and `After`. They all share the signature `(Given pattern binds & body)`, where the binds are `[state & args]` with the args matching the number of arguments detailed in the pattern.

The binds and body are turned into a function. The functions are executed in a `lazytest.test-case/test-case`, so `expect` calls (and other assertions) properly work, and they _must_ return the state variable (or else throws).

For example:

```clojure lazytest/skip=true
(ns step-definitions.order
  (:require
   [lazytest.core :refer [expect]]
   [lazytest.extensions.cucumber :refer [Given Then When]]))

(Given "there is a list" [state]
  (assoc state ::list []))

(When "I append {int} to the list" [state n]
  (update state ::list conj n))

(Then "the list should be {int} long" [state value]
  (expect (= (count (::list state)) value))
  state)
```

With the steps defined, the feature can be written in a file with `.feature` extension, using the Gherkin syntax:

```gherkin
Feature: Steps are executed one by one
  Scenario: Executed step by step
    Given there is a list
    When I append 1 to the list
    And I append 2 to the list
    And I append 3 to the list
    Then the list should be 3 long
```

Then when executing lazytest, pass in the feature directory and the step directory. Both will be picked up, each feature will be converted to a lazytest test suite and added to the test run.

```bash
$ clojure -M:dev:test --hook lazytest.extensions.cucumber/hook --cucumber-features test/features --cucumber-steps test/clojure/step_definitions

...
Cucumber Tests
  Feature: Steps are executed one by one
    √ Scenario: Executed step by step

Ran 123 test cases in 0.55025 seconds.
0 failures.
```
