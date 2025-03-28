default:
    @just --list

today := `date +%F`
current_version := `cat resources/LAZYTEST_VERSION | xargs`

# Set version, change all instances of <<next>> to version
@set-version version:
    echo '{{version}}' > resources/LAZYTEST_VERSION
    fd '.(clj|edn|md)' . -x sd '<<next>>' '{{version}}' {}
    sd '{{current_version}}' '{{version}}' README.md
    sd '{{current_version}}' '{{version}}' build.clj
    sd '## Unreleased' '## Unreleased\n\n## {{version}}\n\nReleased `{{today}}`.' CHANGELOG.md

@gen-docs:
    markdown-toc -i --maxdepth 2 README.md

clojure-lsp:
    clojure-lsp diagnostics

splint:
    clojure -M:provided:dev:test:splint

[no-exit-message]
test *args:
    clojure -T:prep javac
    clojure -M:provided:dev:test:run {{args}}

[no-exit-message]
test-all *args:
    @just clojure-lsp
    @just splint
    @just test --doctests --md README.md --dir docs --dir test {{args}}

repl arg="":
    clojure -T:prep javac
    clojure -M:provided:dev:test{{arg}}:repl

@clojars:
    env CLOJARS_USERNAME='noahtheduke' CLOJARS_PASSWORD=`cat ../clojars.txt` clojure -T:build deploy

# Builds the uberjar, builds the jar, sends the jar to clojars
@release version:
    echo 'Running tests'
    just test-all
    echo 'Setting new version {{version}}'
    just set-version {{version}}
    echo 'Commit and tag'
    just gen-docs
    git commit -a -m 'Bump version for release'
    git tag v{{version}}
    echo 'Pushing to github'
    git push
    git push --tags
    echo 'Building uber'
    clojure -T:build uberjar
    echo 'Deploying to clojars'
    just clojars
