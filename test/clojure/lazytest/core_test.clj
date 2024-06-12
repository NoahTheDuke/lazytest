(ns lazytest.core-test
  (:require
   [lazytest.core :refer [causes-with-msg? causes? defdescribe describe expect
                          expect-it given it throws-with-msg? throws?]])
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
        (expect (= '(= 1 2) (:expected (ex-data e)))))))
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
    (it "wraps other throwables"
      (try
        (expect (throws? ExceptionInfo #(assert false)))
        (catch ExpectationFailed ef
          (expect (instance? AssertionError (:caught (ex-data ef))))))))
  (describe throws-with-msg?
    (it "checks the thrown message"
      (expect (throws-with-msg? Exception #"foo message"
                #(do (throw (Exception. "the foo message for this exception")))))))
  (describe causes?
    (expect-it "checks the base throwable"
      (causes? IllegalArgumentException
               #(throw (IllegalArgumentException. "bad arguments"))))
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
  (given [e1 (try (expect (= 1 (inc 2)))
               false
               (catch ExpectationFailed err err))
          reason (ex-data e1)]
    (it "is correctly formed"
      (expect (some? e1))
      (expect (= '(= 1 (inc 2)) (:expected reason)))
      (expect (= (list = 1 3) (:evaluated reason)))
      (expect (false? (:actual reason)))))
  (given [e3 (try (expect (instance? java.lang.String (+ 40 2)))
               false
               (catch ExpectationFailed err err))
          reason (ex-data e3)]
    (it "is correctly formed"
      (expect (some? e3))
      (expect (= '(instance? java.lang.String (+ 40 2)) (:expected reason)))
      (expect (= (list instance? java.lang.String 42) (:evaluated reason)))
      (expect (false? (:actual reason))))))
