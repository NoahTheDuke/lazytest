default:
    @just --list

today := `date +%F`
current_version := `cat resources/LAZYTEST_VERSION | xargs`

# Set version, change all instances of <<next>> to version
@set-version version:
    echo '{{version}}' > resources/LAZYTEST_VERSION
    fd '.(clj|edn|md)' . -x sd '<<next>>' '{{version}}' {}
    sd '{{current_version}}' '{{version}}' README.md
    sd '## Unreleased' '## Unreleased\n\n## {{version}}\n\nReleased `{{today}}`.' CHANGELOG.md

[no-exit-message]
test *args:
    clojure -T:prep javac
    clojure -M:provided:dev:test:run {{args}}

[no-exit-message]
test-all *args:
    clj-kondo  --parallel --lint dev src test
    just test {{args}}

repl arg="":
    clojure -T:prep javac
    clojure -M:provided:dev:test{{arg}}:repl

@clojars:
    env CLOJARS_USERNAME='noahtheduke' CLOJARS_PASSWORD=`cat ../clojars.txt` clojure -M:clein deploy
