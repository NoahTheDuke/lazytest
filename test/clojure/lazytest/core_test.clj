(ns lazytest.core-test
  (:require
   [lazytest.core :refer [causes-with-msg? causes? defdescribe describe expect
                          given it throws-with-msg? throws?]])
  (:import
   clojure.lang.ExceptionInfo
   lazytest.ExpectationFailed))

(defdescribe it-test
  (it "will early exit"
    (try
      ((it "when given multiple expressions"
         (expect (= 1 2))
         (throw (ex-info "never reached" {}))))
      (catch ExpectationFailed e
        (expect (= '(= 1 2) (:form (ex-data e)))))))
  (let [state (atom 0)]
    (it "arbitrary code"
      (expect (= 4 (+ 2 2)))
      (swap! state inc)
      (expect (= 1 @state)))))

(defn plus [a b] (+ a b))

(defdescribe with-redefs-test
  (it "should be rebound"
    (with-redefs [plus *]
      (expect (= 6 (plus 2 3)))))
  (describe "redefs outside 'it' blocks"
    (with-redefs [plus *]
      (it "should not be rebound"
        (expect (not= 6 (plus 2 3)))))))

(defdescribe expect-helpers-test
  (describe throws?
    (it "catches expected throwable class"
      (expect (throws? ExceptionInfo #(throw (ex-info "expected exception" {})))))
    (it "catches Throwables"
      (expect (throws? AssertionError #(assert false))))
    (it "fails if function doesn't throw"
      (expect (false? (throws? ExceptionInfo #(do nil)))))
    (it "rethrows other errors"
      (try
        (expect (throws? ExceptionInfo #(assert false)))
        (catch AssertionError _))))
  (describe throws-with-msg?
    (it "checks the thrown message"
      (expect (throws-with-msg? Exception #"foo message"
                #(do (throw (Exception. "the foo message for this exception")))))))
  (describe causes?
    (it "checks the base throwable"
      (expect (causes? IllegalArgumentException
                       #(throw (IllegalArgumentException. "bad arguments")))))
    (it "checks the causes too"
      (expect (causes? IllegalArgumentException
                       #(try
                          (throw (IllegalArgumentException. "bad stuff"))
                          (catch IllegalArgumentException e
                            (throw (RuntimeException. "wrapped stuff" e))))))))
  (describe causes-with-msg?
    (it "checks the base throwable"
      (expect (causes-with-msg? ExceptionInfo
                #"foo"
                #(throw (ex-info "foo bar" {})))))
    (it "checks nested causes"
      (expect (causes-with-msg? ExceptionInfo
                #"worser message"
                #(throw (IllegalArgumentException.
                          "bad argument"
                          (ex-info "foo message" {:extra :data}
                                   (ex-info "worser message" {:foo :bar})))))))))

(defdescribe expect-data-test
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
