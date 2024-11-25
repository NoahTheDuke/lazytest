(ns lazytest.extensions.expectations-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [lazytest.core :refer [expect it throws-with-msg? throws?]]
   [lazytest.extensions.expectations :as sut :refer [from-each in more more->
                                                     more-of]]) 
  (:import
   (clojure.lang ExceptionInfo)
   (lazytest ExpectationFailed)))

(s/def ::pos pos?)

(sut/defexpect simple-test 1 1)

(sut/defexpect simple-call-test (+ 2 4) (+ 2 2 2))

(sut/defexpect either-expect-works
  (it "handles lazytest" (expect (= 1 1)))
  (it "handles expectations" (sut/expect (= 2 2))))

(sut/defexpect expectations-test
  (it "value" (sut/expect true))
  (it "=" (sut/expect 1 1))
  (it "fn?" (let [i 1] (sut/expect pos? i)))
  (it "regex" (sut/expect #"hello" "hello world"))
  (it "instance?" (sut/expect String "hello world"))
  (it "catch" (sut/expect ExceptionInfo (throw (ex-info "aw shucks" {}))))
  (it "spec" (sut/expect ::pos 1)))

(sut/defexpect expectations
  (it "prints correctly"
    (expect (throws-with-msg? ExpectationFailed
              #"did not satisfy"
              #(sut/expect even? (+ 1 1 1) "It's uneven!")))))

(sut/defexpect expectations-examples-test
  (it "handles the fancy stuff too"
    (sut/expect (more-> ArithmeticException type #"Divide by zero" ex-message) (/ 12 0))
    (sut/expect {:foo 1} (in {:foo 1 :cat 4}))
    (sut/expect #{1 2} (in #{0 1 2 3}))
    (expect (throws? ExpectationFailed
                     #(sut/expect :foo (in #{:foo :bar}))))
    (expect (throws? ExpectationFailed
                     #(sut/expect :foo (in [:bar :foo]))))
    (sut/expect (more-of {:keys [a b]}
                         even? a
                         odd?  b)
                {:a (* 2 13) :b (* 3 13)})
    (sut/expect (more #"foo" "foobar" #(str/starts-with? % "f"))
                (str "f" "oobar"))
    (sut/expect #"foo"
                (from-each [s ["l" "d" "bar"]]
                  (str "foo" s)))
    (let [counter (atom 1)]
      ;; expectations issue 24: more should only evaluate actual expression once:
      (sut/expect (more even? even? even? pos?)
                  (swap! counter inc))
      (expect (= 2 @counter)))
    (let [counter (atom 1)]
      ;; expectations issue 24: more-> should only evaluate actual expression once:
      (sut/expect (more-> even? identity even? identity even? identity pos? identity)
                  (swap! counter inc))
      (expect (= 2 @counter)))))

(sut/defexpect more-of-test
  (it "lazytest issue 13: can handle big more-ofs"
    (sut/expect
     (more-of [_ url site kw nationality {:keys [country state city]} locale]
              "/"          url
              {}           site
              {}           kw
              "greek"      nationality
              "US"         country
              "California" state
              vector?      city
              3            (count city)
              "Castro Valley" (first city)
              number?      (second city)
              number?      (last city)
              "en_US"      locale)
     (from-each [call [[nil
                        "/"
                        {}
                        {}
                        "greek"
                        {:country "US"
                         :state "California"
                         :city ["Castro Valley" 1 2]}
                        "en_US"]]]
       call))))
