# Hooks (Plugins)

Lazytest supports writing plugins, called hooks, which can modify the state of a run while it is being executed. The authors of Lazytest can't predict every need, so we've added hooks as a means of "hooking" into the runtime system. Hooks are functions (generally multimethods) that dispatch on a keyword to do different things (set custom data, print info, change something about the data).

<!-- toc -->

- [Using Existing Hooks](#using-existing-hooks)
- [Writing Custom Hooks](#writing-custom-hooks)
  * [`defhook`](#defhook)
  * [Manually writing a hook function](#manually-writing-a-hook-function)

<!-- tocstop -->

## Using Existing Hooks

Hooks can be used with the `--hook` option, which can be given multiple times. For example, `clojure -M:dev:test:lazytest --hook custom.ns/foo --hook custom.ns/bar` will load and include both `custom.ns/foo` and `custom.ns/bar` as hooks during the run. Like reporters, if the specified symbol is not fully-qualified, it will be assumed to exist in `lazytest.hooks`, which is where the built-in hooks live.

Each hook can provide its own cli options, so it can be helpful to call `clojure ... --hook custom.ns/foo --help` to see what options are made available by the included hooks.

The two built-in hooks ([`lazytest.hooks/profiling`][profiling], [`lazytest.hooks/randomize`][randomize]) run by default when included with `--hook`, but they can be included and disabled by passing in the right flag. This is a deliberate design choice and not every hook will work like this. (Please see each hooks' docs for further details.)

[profiling]: docs/hooks/profiling.md
[randomize]: docs/hooks/randomize.md

## Writing Custom Hooks

### `defhook`

The primary means of writing a custom hooks is the helper macro [`lazytest.hooks/defhook`][defhook]. It creates a multimethod with the built-in hook dispatch function, defines a no-op `:default`, and has syntax similar to `extend-protocol`. The available hook methods are defined by the hook keywords, listed below. Each takes a `config` map and a `source` object. (Unlike manually created hook functions, the `hook-type` argument is inserted for you.)

[defhook]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.hooks#defhook

```clojure
(require '[lazytest.hooks :refer [defhook]])

(defhook yell
  "Prints a message at the start and end of the whole run."
  (cli-opts [config opts]
    (into opts
      [[nil "--[no-]yell" "Yell loudly"
        :id :yell/enabled
        :default true]]))
  (pre-test-run
    [config m]
    (println "STARTING")
    m)
  (post-test-run
    [config m]
    (println "ENDING")
    m))

(with-out-str (yell nil {:random :object} :post-test-run))
;; => "ENDING\n"
```

The current set of hook keywords, along with relevant details:

* `:cli-opts`: Called before `config` is built or cli opts have been fully parsed. Input is a vector of cli options.
* `:config`: Called after reporters and hooks have been resolved. Input is the `config` map.
* `:pre-test-run`: Called before the runner has started a full run. Input is the entire run's `suite`.
* `:post-test-run`: Called after the runner has finished a full run. Input is a `suite-result`, with the original `suite` stored under `:source`.
* `:pre-test-suite`: Called for every `suite` (namespace, var, nested suites). Input is the suite.
* `:post-test-suite`: Called after each `suite` (namespace, var, nested suites) has run. Input is a `suite-result`, with the original `suite` stored under `:source`.
* `:pre-test-case`: Called for every `test-case` that is executed. Input is the `test-case`.
* `:post-test-case`: Called after each `test-case` is executed. Input is the `test-case-result`, with the original `test-case` stored under `:source`.

### Manually writing a hook function

Hooks are merely functions that are called with the same 3 arguments: the `config` map, a `source` object, and the `hook-type` keyword . The `hook-type` keyword is used as the dispatch value, and will be limited to the existing set of hook keywords (as listed above). It is expected that a given hook function will do different things based on `hook-type`, but that is not always the case.

The hook function should return `nil` (indicating a no-op) or the `source` object, modified as desired.

```clojure lazytest/skip=true
(defmulti example-hook-mm {:arglists '([config m hook-type])} #'lazytest.hooks/hook-dispatch)
(defmethod example-hook-mm :default [config m _] m)
(defmethod example-hook-mm :config [config config _] ...)
(defmethod example-hook-mm :pre-test-run [config suite _] ...)

(defn example-hook-func [obj config hook-type]
  (case hook-type
    :config ...
    :pre-test-run ...
    #_:else nil))
```
