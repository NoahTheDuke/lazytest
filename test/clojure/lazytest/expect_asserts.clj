(ns lazytest.expect-asserts
  (:require
    [lazytest.expect :refer [expect]]
    [lazytest.expect.thrown :refer [throws? throws-with-msg? causes?]]
    [lazytest.describe :refer [describe given it]]) 
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

(describe expect-data-test
  (given [e1 (try (expect (= 1 2))
               false
               (catch ExpectationFailed err err))
          reason (ex-data e1)]
    (it "is correctly formed"
      (expect e1)
      (expect (= '(= 1 2) (:form reason)))
      (expect (= (list = 1 2) (:evaluated reason)))
      (expect (false? (:result reason)))))
  (given [e3 (try (expect (instance? java.lang.String 42))
               false
               (catch ExpectationFailed err err))
          reason (ex-data e3)]
    (it "is correctly formed"
      (expect e3)
      (expect (= '(instance? java.lang.String 42) (:form reason)))
      (expect (= (list instance? java.lang.String 42) (:evaluated reason)))
      (expect (false? (:result reason))))))
