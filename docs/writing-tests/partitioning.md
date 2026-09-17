# Partitioning Individual Tests and Suites

All of the test suite and test case macros (`defdescribe`, `describe`, `it`, `expect-it`) take a metadata map after the docstring. Adding `:focus true` to this map will cause *only* that test/suite to be run. Removing it will return to the normal behavior (run all tests).

```clojure lazytest/skip=true
(defdescribe focus-test
  (it "will be run"
    {:focus true}
    (expect (= 1 2)))
  (it "will be skipped"
    (expect (= 1 1))))
```

And adding `:skip true` to the metadata map will cause that test/suite to be *not* run:

```clojure lazytest/skip=true
(defdescribe skip-test
  (it "will be skipped"
    {:skip true}
    (expect (= 1 2)))
  (it "will be run"
    (expect (= 1 1))))
```

> [!NOTE]
> `:skip` overrides `:focus`, so `{:focus true :skip true}` will be skipped.

Additionally, you can use the cli option `-n`/`--namespace` to specify one or more namespaces to focus wholly, or you can use the cli option `-v`/`--var` to specify one or more fully-qualified vars to focus. This allows for testing from the command line without modifying source files.

To partition your test suite based on metadata, you can use `-i`/`--include` to only run tests with the given metadata, or `-e`/`--exclude` to skip tests with the given metadata.
