(ns lazytest.extensions.matcher-combinators-test
  (:require
   [lazytest.core :refer [defdescribe describe expect it]]
   [lazytest.extensions.matcher-combinators :refer [match? thrown-match?]]
   [lazytest.expectation-failed :refer [ex-failed?]]))

(defdescribe matchers-test
  (describe "correctly mirrors the assert-expr functionality"
    (it match?
      (expect (match? {:foo 1} {:foo 1}))
      (try (expect (match? {:foo 1} {:foo :bar}))
           (catch Throwable ex
             (if (ex-failed? ex)
               (expect (= '(match? {:foo 1} {:foo :bar})
                          (:expected (ex-data ex))))
               (throw ex)))))
    (it thrown-match?
      (expect (thrown-match? {:foo 1}
                             (throw (ex-info "heck" {:foo 1}))))
      (try (expect (thrown-match? clojure.lang.ExceptionInfo
                                  {:foo 1}
                                  (assert false "asdf")))
           (catch Throwable ex
             (if (ex-failed? ex)
               (= "asdf" (ex-message (ex-data ex)))
               (throw ex)))))))
