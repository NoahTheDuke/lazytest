# Editor Integration

The entry-points are at `lazytest.repl`: `run-all-tests`, `run-tests`, and `run-test-var`. The first runs all loaded test namespaces, the second runs the provided namespaces (either a single namespace or a collection of namespaces), and the third runs a single test var. If your editor can define custom repl functions, then it's fairly easy to set these as your test runner.

## Neovim

Neovim with [Conjure](https://github.com/Olical/conjure):

```lua
-- in your init.lua
local runners = require("conjure.client.clojure.nrepl.action")
runners["test-runners"].lazytest = {
  ["namespace"] = "lazytest.repl",
  ["all-fn"] = "run-all-tests",
  ["ns-fn"] = "run-tests",
  ["single-fn"] = "run-test-var",
  ["default-call-suffix"] = "",
  ["name-prefix"] = "#'",
  ["name-suffix"] = ""
}
vim.g["conjure#client#clojure#nrepl#test#runner"] = "lazytest"
vim.g["conjure#client#clojure#nrepl#test#current_form_names"] = { "describe" }
```

## VSCode

VSCode with [Calva](https://calva.io/custom-commands):

```json
"calva.customREPLCommandSnippets": [
    {
        "name": "Lazytest: Test All Tests",
        "snippet": "(lazytest.repl/run-all-tests)"
    },
    {
        "name": "Lazytest: Test Current Namespace",
        "snippet": "(lazytest.repl/run-tests $editor-ns)"
    },
    {
        "name": "Lazytest: Test Current Var",
        "snippet": "(lazytest.repl/run-test-var #'$top-level-defined-symbol)"
    }
],
```

## IntelliJ

IntelliJ with [Cursive](https://cursive-ide.com/userguide/repl.html#repl-commands):

```
Name: Lazytest - Test All Tests
Execute Command: (lazytest.repl/run-all-tests)
Execution Namespace: Execute in current file namespace
Results: Print results to REPL output

Name: Lazytest - Test Current Namespace
Execute Command: (lazytest.repl/run-tests ~file-namespace)
Execution Namespace: Execute in current file namespace
Results: Print results to REPL output

Name: Lazytest - Test Current Var
Execute Command: (lazytest.repl/run-test-var #'~current-var)
Execution Namespace: Execute in current file namespace
Results: Print results to REPL output
```
