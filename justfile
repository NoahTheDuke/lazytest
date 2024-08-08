default:
    @just --list

today := `date +%F`
current_version := `cat resources/LAZYTEST_VERSION | xargs`

# Set version, change all instances of <<next>> to version
@set-version version:
    echo '{{version}}' > resources/LAZYTEST_VERSION
    fd '.(clj|edn|md)' . -x sd '<<next>>' '{{version}}' {}
    sd '{{current_version}}' '{{version}}' README.md
    sd '## Unreleased' '## Unreleased\n\n## {{version}} - {{today}}' CHANGELOG.md
