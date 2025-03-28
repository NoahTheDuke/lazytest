(ns lazytest.experimental.interfaces.expectations-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [lazytest.experimental.interfaces.expectations :as sut :refer [from-each in
                                                                  more more->
                                                                  more-of]]
   [lazytest.extensions.expectations :refer [expect]]) 
  (:import
   (clojure.lang ExceptionInfo)
   (lazytest ExpectationFailed)))

(s/def ::pos pos?)

(sut/defexpect expectations-test
  (sut/expecting "value" (sut/expect true))
  (sut/expecting "=" (sut/expect 1 1))
  (sut/expecting "fn?" (let [i 1] (sut/expect pos? i)))
  (sut/expecting "regex" (sut/expect #"hello" "hello world"))
  (sut/expecting "instance?" (sut/expect String "hello world"))
  (sut/expecting "catch" (sut/expect ExceptionInfo (throw (ex-info "aw shucks" {}))))
  (sut/expecting "spec" (sut/expect ::pos 1)))

(sut/defexpect expectations-examples-test
  (sut/expecting "handles the fancy stuff too:"
    (sut/expecting "in"
      (sut/expect {:foo 1} (in {:foo 1 :cat 4}))
      (sut/expect #{1 2} (in #{0 1 2 3}))
      (expect ExpectationFailed
        (#(sut/expect :foo (in #{:foo :bar}))))
      (expect ExpectationFailed
        (#(sut/expect :foo (in [:bar :foo])))))
    (sut/expecting "from-each"
      (sut/expect #"foo"
        (from-each [s ["l" "d" "bar"]]
          (str "foo" s))))
    (sut/expecting "more"
      (sut/expect (more #"foo" "foobar" #(str/starts-with? % "f"))
        (str "f" "oobar"))
      (let [counter (atom 1)]
        ;; expectations issue 24: more should only evaluate actual expression once:
        (sut/expect (more even? even? even? pos?)
          (swap! counter inc))
        (expect (= 2 @counter))))
    (sut/expecting "more->"
      (sut/expect (more-> ArithmeticException type #"Divide by zero" ex-message) (/ 12 0))
      (let [counter (atom 1)]
        ;; expectations issue 24: more-> should only evaluate actual expression once:
        (sut/expect
          (more-> even? identity even? identity even? identity pos? identity)
          (swap! counter inc))
        (expect (= 2 @counter))))
    (sut/expecting "more-of"
      (sut/expect (more-of {:keys [a b]}
                           even? a
                           odd?  b)
        {:a (* 2 13) :b (* 3 13)}))))
