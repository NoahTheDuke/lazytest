(ns examples.random-test
  (:require
    [lazytest.describe :refer [describe for-any it]]
    [lazytest.random :as r]))

(describe string-of
  (for-any [s (r/string-of (r/pick r/letter r/digit))]
    (it "is a string"
      (string? s))
    (it "has only letters and digits"
      (every? #(re-matches #"[0-9A-Za-z]" (str %)) s))))
