# Extensions

Lazytest includes a number of extension namespaces, each of which either add integration with an external library (which requires adding the library as an additional dependency) or makes functionality available which is challenging to use or goes against the standard Lazytest behavior.

* [Matcher Combinators](docs/extensions/matcher-combinators.md) - Adds `match?` and `thrown-match?` macros to integrate with [nubank/matcher-combinators].
* [Cucumber Tests](docs/extensions/cucumber.md) - Adds support for [Cucumber-style tests](https://cucumber.io), relying on [lambdaisland/kaocha-cucumber](https://github.com/lambdaisland/kaocha-cucumber).
* [Expectations v2-style assertions](docs/extensions/expectations-v2.md) - `expect` assertion that works like the [Expectations v2](https://github.com/clojure-expectations/clojure-test) assertion.
