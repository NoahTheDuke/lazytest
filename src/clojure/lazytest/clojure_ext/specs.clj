(ns lazytest.clojure-ext.specs
  "Copying some core.specs.alpha specs for use in babashka" 
  (:require
    [clojure.spec.alpha :as s]))

(set! *warn-on-reflection* true)

(s/def ::local-name (s/and simple-symbol? #(not= '& %)))

(s/def ::binding-form
  (s/or :local-symbol ::local-name
    :seq-destructure ::seq-binding-form
    :map-destructure ::map-binding-form))

;; sequential destructuring

(s/def ::seq-binding-form
  (s/and vector?
    (s/cat :forms (s/* ::binding-form)
      :rest-forms (s/? (s/cat :ampersand #{'&} :form ::binding-form))
      :as-form (s/? (s/cat :as #{:as} :as-sym ::local-name)))))

;; map destructuring

(s/def ::keys (s/coll-of ident? :kind vector?))
(s/def ::syms (s/coll-of symbol? :kind vector?))
(s/def ::strs (s/coll-of simple-symbol? :kind vector?))
(s/def ::or (s/map-of simple-symbol? any?))
(s/def ::as ::local-name)

(s/def ::map-special-binding
  (s/keys :opt-un [::as ::or ::keys ::syms ::strs]))

(s/def ::map-binding (s/tuple ::binding-form any?))

(s/def ::ns-keys
  (s/tuple
    (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
    (s/coll-of simple-symbol? :kind vector?)))

(s/def ::map-bindings
  (s/every (s/or :map-binding ::map-binding
             :qualified-keys-or-syms ::ns-keys
             :special-binding (s/tuple #{:as :or :keys :syms :strs} any?)) :kind map?))

(s/def ::map-binding-form (s/merge ::map-bindings ::map-special-binding))

(s/def ::param-list
  (s/and
    vector?
    (s/cat :params (s/* ::binding-form)
      :var-params (s/? (s/cat :ampersand #{'&} :var-form ::binding-form)))))
