# Doc Tests

Lazytest can run tests in code blocks of your markdown files with `--md FILE`. It looks for any triple backtic-delimited code block that has `clojure` or `clj` as the language specifier, and that doesn't have `lazytest/skip=true` in the info-string, bundles it into a standalone [`describe`][describe] block, and then runs all of the suites as a single suite under the name of the markdown file.

It determines what should be considered a test (`(expect (= x y))`) by the presence of `=>`. Code immediately before a line containing `=>` (leading `;` optional) is treated as the actual, and the value after treated as the expected result.

This will run:

````markdown
```clojure
(defn adder [a b]
  (+ a b))

(adder 5 6)
;; => 11
```
````

Whereas these will not (first is skipped, second isn't "clojure" or "clj"):

````markdown
```clojure lazytest/skip=true
(System/exit 1)
;; => exit!!!
```

```clojurescript
print("Hello world!")
```
````

Additionally, a custom string can be used instead of the default (headers from the markdown file) by using the info-string `lazytest/describe`:


````markdown
```clojure lazytest/describe=easy-adder
(+ 5 6)
;; => 11
```
````

will be printed as:

```markdown
  readme-md
    easy-adder
      √ Doc Tests
```

[describe]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.core#describe
