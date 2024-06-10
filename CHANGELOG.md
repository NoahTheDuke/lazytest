# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## Unreleased

## 0.1.0 - 2024-06-09

Updated original code to use deps.edn, tools.build, and other modern tooling.

### Big changes

* Remove all maven-specific and leiningen-specific code.
* Remove `clojure.test`-like api.
* Remove random sampling code.
* Remove dependency-tracking and reloading and autotest code.
* Remove clojure 1.3 test code examples.
* Add CLI api.
* Change external API to use a single namespace (`lazytest.core`).
* Change external API macros and functions to work better with modern tooling (use vars, etc).
