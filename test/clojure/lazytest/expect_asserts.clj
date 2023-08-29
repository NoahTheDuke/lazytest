(ns lazytest.expect-asserts
  (:require
    [lazytest.expect :refer [expect]]
    [lazytest.expect.thrown :refer [throws? throws-with-msg? causes?]]) 
  (:import
    (lazytest ExpectationFailed)))

(set! *warn-on-reflection* true)

(expect (= 1 1))
(expect (not= 1 2))
(expect (instance? java.lang.String "Hello, World!"))

(expect (throws? Exception #(do (throw (IllegalArgumentException.)))))
(expect (throws-with-msg? Exception #"foo message"
          #(do (throw (Exception. "the foo message for this exception")))))

(expect (causes? IllegalArgumentException
          #(do (throw (IllegalArgumentException. "bad arguments")))))

(expect (causes? IllegalArgumentException
          #(do (try
                 (throw (IllegalArgumentException. "bad stuff"))
                 (catch IllegalArgumentException e
                   (throw (RuntimeException. "wrapped stuff" e)))))))

(let [e1 (try (expect (= 1 2))
           false
           (catch ExpectationFailed err err))]
  (assert e1)
  (let [reason (.reason ^ExpectationFailed e1)]
    (assert (= '(= 1 2) (:form reason)))
    (assert (= (list = 1 2) (:evaluated reason)))
    (assert (false? (:result reason)))))

(let [e3 (try (expect (instance? java.lang.String 42))
           false
           (catch ExpectationFailed err err))]
  (assert e3)
  (let [reason (.reason ^ExpectationFailed e3)]
    (assert (= '(instance? java.lang.String 42) (:form reason)))
    (assert (= (list instance? java.lang.String 42) (:evaluated reason)))
    (assert (false? (:result reason)))))
