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

prep:
    clojure -X:deps prep

clean:
    clojure -T:build clean

compile:
    clojure -T:build compile-clojure :clean true

jar:
    clojure -T:build jar

uberjar:
    clojure -T:build uberjar

alias uber := uberjar

lint:
    @just clojure-lsp
    @just splint

test-bb *args:
    @just prep
    bb lazytest {{args}}

[no-exit-message]
test-raw *args:
    clojure -M:provided:dev:test:run {{args}}

[no-exit-message]
test *args:
    @just prep
    @just test-raw {{args}}

[no-exit-message]
test-all *args:
    @just clojure-lsp
    @just splint
    @just compile
    @just prep
    bb lazytest --doctests --md README.md --dir docs --dir test --output results --output summary {{args}}
    @just test-raw --doctests --md README.md --dir docs --dir test --output results --output summary {{args}}

repl arg="":
    @just prep
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
    @just uberjar
    echo 'Deploying to clojars'
    just clojars
