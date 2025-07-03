(ns lazytest.core-test
  (:require
   [lazytest.core :refer [cause-seq causes-with-msg? causes? context
                          defdescribe describe expect expect-it it ok? should
                          specify throws-with-msg? throws?]]
   [lazytest.extensions.matcher-combinators :refer [match?]]
   [lazytest.expectation-failed :refer [ex-failed?]])
  (:import
   clojure.lang.ExceptionInfo
   [java.util.regex Pattern]))

(set! *warn-on-reflection* true)

(defdescribe it-test
  (it "will early exit"
    (try
      ((:body (it "when given multiple expressions"
                (expect (= 1 2))
                (throw (ex-info "never reached" {})))))
      (catch Throwable e
        (if (ex-failed? e)
          (expect (= '(= 1 2) (:expected (ex-data e))))
          (throw e)))))
  (let [state (atom 0)]
    (it "arbitrary code"
      (expect (= 4 (+ 2 2)))
      (swap! state inc)
      (expect (= 1 @state)))))

(defn plus [a b] (+ a b))

(defdescribe with-redefs-test
  (describe "redefs inside 'it' blocks"
    (it "should be rebound"
      (with-redefs [plus *]
        (expect (= 6 (plus 2 3)) "this should be true"))))
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
        (catch Throwable ef
          (if (ex-failed? ef)
            (expect (instance? AssertionError (:caught (ex-data ef))))
            (throw ef))))))
  (describe throws-with-msg?
    (it "checks the thrown message"
      (expect (throws-with-msg? Exception #"foo message"
                #(do (throw (Exception. "the foo message for this exception")))))
      (try (throws-with-msg? Exception #"bar message"
                #(do (throw (Exception. "the foo message for this exception"))))
           (expect nil "Should have failed")
           (catch Throwable ex
             (if (ex-failed? ex)
               (expect
                 (match? {:message "java.lang.Exception found but not with expected message"
                          :expected
                          (list 're-find
                                #(instance? Pattern %)
                                "the foo message for this exception")
                          :actual "the foo message for this exception"}
                         (ex-data ex)))
               (throw ex))))))
  (describe causes?
    (expect-it "checks the base throwable"
      (causes? IllegalArgumentException
               #(throw (IllegalArgumentException. "bad arguments"))))
    (it "checks the causes too"
      (expect (causes? IllegalArgumentException
                       #(throw (RuntimeException. "wrapped stuff"
                                                  (IllegalArgumentException. "bad stuff")))))))
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
                                   (ex-info "worser message" {:foo :bar}))))))
      (try (causes-with-msg? ExceptionInfo
                #"worsest message"
                #(throw (IllegalArgumentException.
                          "bad argument"
                          (ex-info "foo message" {:extra :data}
                                   (ex-info "worser message" {:foo :bar})))))
           (expect nil "Should have failed")
           (catch Throwable ex
             (if (ex-failed? ex)
               (expect
                 (match? {:message "clojure.lang.ExceptionInfo found but not with expected message"
                          :expected
                          (list 're-find
                                #(instance? Pattern %)
                                "foo message")
                          :actual "foo message"}
                         (ex-data ex)))
               (throw ex))))))
  (describe ok?
    (it "returns true"
      (expect (true? (expect (ok? (constantly false))))))
    (it "doesn't catch thrown exceptions"
      (try (expect (ok? #(throw (ex-info "ok?" {:foo :bar}))))
           (catch Throwable ex
             (if (ex-failed? ex)
               (let [caught (-> ex ex-data :caught)]
                 (expect (instance? clojure.lang.ExceptionInfo caught))
                 (expect (= "ok?" (ex-message caught)))
                 (expect (= {:foo :bar} (ex-data caught))))
               (throw ex)))))))

(defdescribe expect-data-test
  (it "is correctly formed"
    (let [e1 (try (expect (= 1 (inc 2)))
                  false
                  (catch Throwable err
                    (if (ex-failed? err) err (throw err))))
          reason (ex-data e1)]
      (expect (some? e1))
      (expect (= '(= 1 (inc 2)) (:expected reason)))
      (expect (= (list = 1 3) (:evaluated reason)))
      (expect (false? (:actual reason)))))
  (it "is correctly formed"
    (let [e3 (try (expect (instance? java.lang.String (+ 40 2)))
                  false
                  (catch Throwable err
                    (if (ex-failed? err) err (throw err))))
          reason (ex-data e3)]
      (expect (some? e3))
      (expect (= '(instance? java.lang.String (+ 40 2)) (:expected reason)))
      (expect (= (list instance? java.lang.String 42) (:evaluated reason)))
      (expect (false? (:actual reason))))))

(defdescribe alternative-assertions
  (it "can handle `ex-info`"
    (try
      ((:body (it "ex-info example"
                (let [f (when-not (= 1 2)
                          (throw (ex-info "not equal" {:extra :data})))]
                  (f)))))
      (catch ExceptionInfo e
        (expect (= "not equal" (ex-message e)))
        (expect (= {:extra :data} (ex-data e))))))
  (it "can handle `assert`"
    (try
      ((:body (it "assert with no doc"
                (assert (= 1 2)))))
      (catch AssertionError e
        (expect (= "Assert failed: (= 1 2)" (ex-message e)))))
    (try
      ((:body (it "assert with no doc"
                (assert (= 1 2) "these should be equal"))))
      (catch AssertionError e
        (expect (= "Assert failed: these should be equal\n(= 1 2)" (ex-message e)))))))

(defdescribe context-test
  (context "this works correctly"
    (specify "inside works too"
      (should (= 1 1)))))

(defdescribe cause-seq-test
  (expect-it "returns a lazy-seq"
    (seq? (cause-seq (ex-info "" {}))))
  (expect-it "works outermost inward"
    (= [clojure.lang.ExceptionInfo Exception IllegalArgumentException]
       (map class (cause-seq
                     (ex-info "" {} (Exception. "" (IllegalArgumentException. ""))))))))
