# Hook: Randomize

By default, Lazytest will run all tests in the order they're found, which adheres pretty closely to a depth-first traversal of the test directory. This isn't always desireable, so to help combat that, the hook `lazytest.hooks/randomize` has been written to allow you to change the order.

Once the hook is included with `--hook lazytest.hooks/randomize`, it will randomize everything by default. However, the level of randomization can be changed with `--randomize TYPE`. This randomizes the order of runs, with available types: `all`, `ns`, `var`, `suite`, or `none` to disable the feature (for example, if the hook is added programatically but you desire to suppress it).
