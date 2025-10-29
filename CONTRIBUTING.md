# Contributing

I'm putting this at the top because it's real important to me: Do not use any generative AI when contributing to this project in any form. This is on the honor system but I'm trusting you here. Don't fuckin do it.

If you want to contribute to `lazytest`, I am eternally grateful. This is a labor of love and it's invigorating to have others help out.

* Issues are always welcome, a good way to help discover edge cases and incorrect impls.
* Bug fixes and small touch ups can be put in a PR, no problem. (Documentation updates are also fair game.)
* For anything bigger, I prefer to have an issue first so we can discuss the scope and nature of the bug, feature, or idea you have.

## Bugs and Feature Requests

* Check that the issue doesn't already exist.
* Check that the issue hasn't already been fixed (but not released yet).
* Keep your issue focused on a problem statement and associated solutions. General discussions are better suited for Clojurian slack.
* If writing about a bug, include the version you're using.

And lastly, **remember that I am human and I build things for myself**. I love to help but I am not being paid to work on this or any other library, so any work I do will come after all of my other responsibilities (to my job, my family, myself), and I will not suffer abuse or demands of my time. Be kind.

## Code Contributions/PRs

* Keep whitespace changes minimal.
* Follow the existing code style in the project.
* Add relevant tests.
* Keep your commits clean and self-contained. No `wip` or `fixes` commits.

PRs are not squashed, so counter to the classic [Utter Disregard For Git Commit History](https://zachholman.com/posts/git-commit-history/), the best course of action is active rebasing to laser-target each commit. If messing with git history is too hard, may I suggest [jujutsu](https://jj-vcs.github.io/jj/latest/)?

## Notes on tests

As this is a test runner, it's expected that you use lazytest to write all tests herein.

#### Matching exceptions

If you wish to match against a test case failure, you can use `(ex-info "message" {...data here...})` in a `match?` call. I've extended the `mc/Matchers` protocol for `ExceptionInfo`, and done some light clean-up so we can get nice output when there's mismatchs. Additionally, you can specify `:type` in the ex-info map, to set the expected exception type.

(See [test/clojure/lazytest/test_utils.clj][] for the details.)
