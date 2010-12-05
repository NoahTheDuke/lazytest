;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Tests for clojure.core/gen-class"
      :author "Stuart Halloway, Daniel Solano Gómez"}
  clojure.test-clojure.genclass
  (:use lazytest.deftest clojure.test-clojure.helpers)
  (:import [clojure.test_clojure.genclass.examples ExampleClass
                                                   ExampleAnnotationClass]
           [java.lang.annotation ElementType
                                 Retention
                                 RetentionPolicy
                                 Target]))

(deftest arg-support
  (let [example (ExampleClass.)
        o (Object.)]
    (is (= "foo with o, o" (.foo example o o)))
    (is (= "foo with o, i" (.foo example o (int 1))))
    (is (thrown? java.lang.UnsupportedOperationException (.foo example o)))))

(deftest name-munging
  (testing "mapping from Java fields to Clojure vars"
    (is (= #'clojure.test-clojure.genclass.examples/-foo-Object-int
           (get-field ExampleClass 'foo_Object_int__var)))
    (is (= #'clojure.test-clojure.genclass.examples/-toString
           (get-field ExampleClass 'toString__var)))))

;todo - fix this, it depends on the order of things out of a hash-map
#_(deftest test-annotations
  (let [annot-class ExampleAnnotationClass
        foo-method          (.getDeclaredMethod annot-class "foo" (into-array [String]))]
    (testing "Class annotations:"
      (is (= 2 (count (.getDeclaredAnnotations annot-class))))
      (testing "@Deprecated"
        (let [deprecated (.getAnnotation annot-class Deprecated)]
          (is deprecated)))
      (testing "@Target([])"
        (let [resource (.getAnnotation annot-class Target)]
          (is (= 0 (count (.value resource)))))))
    (testing "Method annotations:"
      (testing "@Deprecated void foo(String):"
        (is (= 1 (count (.getDeclaredAnnotations foo-method))))
        (is (.getAnnotation foo-method Deprecated))))
    (testing "Parameter annotations:"
      (let [param-annots (.getParameterAnnotations foo-method)]
        (is (= 1 (alength param-annots)))
        (let [first-param-annots (aget param-annots 0)]
          (is (= 2 (alength first-param-annots)))
          (testing "void foo(@Retention(…) String)"
            (let [retention (aget first-param-annots 0)]
              (is (instance? Retention retention))
              (= RetentionPolicy/SOURCE (.value retention))))
          (testing "void foo(@Target(…) String)"
            (let [target (aget first-param-annots 1)]
              (is (instance? Target target))
              (is (= [ElementType/TYPE ElementType/PARAMETER] (seq (.value target)))))))))))

(deftest genclass-option-validation
  (is (fails-with-cause? IllegalArgumentException #"Not a valid method name: has-hyphen"
        (@#'clojure.core/validate-generate-class-options {:methods '[[fine [] void] [has-hyphen [] void]]}))))
