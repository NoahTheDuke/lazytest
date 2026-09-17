# Usage

<!-- toc -->

- [Getting Started](#getting-started)
- [Why a new test framework?](#why-a-new-test-framework)
- [Supported dialects](#supported-dialects)
- [License](#license)

<!-- tocstop -->

With the suggested `:test` alias, call `clojure -M:test [options] [path...]` to run your test suite once, or `clojure -M:test --watch [options] [path...]` to use "Watch mode" (see below) to run repeatedly as files change. `[path...]` here means any file or directory.

* `-d`, `--dir DIR`: Directory containing tests. Can be given multiple times. (Defaults to `test`.)
* `-n`, `--namespace SYMBOL`: Run only the specified test namespaces. Can be given multiple times.
* `-v`, `--var SYMBOL`: Run only the specified fully-qualified symbol. Can be given multiple times.
* `-i`, `--include KEYWORD`: Run only test sequences or vars with this metadata keyword. Can be given multiple times.
* `-e`, `--exclude KEYWORD`: Exclude test sequences or vars with this metadata keyword. Can be given multiple times.
* `--output SYMBOL`: Output format. Can be given multiple times. (Defaults to `nested`.)
* `--hook SYMBOL`: Load and include a hook in the run. (See below to learn more about hooks). Can be given multiple times.
* `--md FILE`: Run doc tests in markdown file. Can be given multiple times. (See [Doc Tests][doc-tests] below.)
* `--watch`: Runs under "Watch mode", which reloads and reruns your test suite as project or test code changes.
* `--delay NUM`: How many milliseconds to wait before checking for changes to reload. Only used in "Watch mode". (Defaults to 500.)
* `--help`: Print help information.
* `--version`: Print version information.

[doc-tests]: docs/doc-tests.md

> [!NOTE]
> If both `--namespace` and `--var` are provided, then Lazytest will run all tests within the namespaces AND the specified vars. They are inclusive, not exclusive.
>
> Additionally, `--exclude` overrides `--include` if both are provided.

## Watch mode

Watch mode uses [clj-reload](https://github.com/tonsky/clj-reload) to reload all local changes on the classpath, plus any files containing namespaces that depend on the changed files. Watch mode defaults to [`lazytest.reporters/dots`][dots] to make the output easier to read. By default, it checks for changes once every 500 milliseconds (1/2 a second), but this can be changed with `--delay`. Watch mode supports all of the other options as well, so you can select a different output style, specific directories, test namespaces, or test vars, etc.

Type `CTRL-C` to stop.

[dots]: https://cljdoc.org/d/io.github.noahtheduke/lazytest/CURRENT/api/lazytest.reporters#dots
