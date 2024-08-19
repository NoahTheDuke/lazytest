(ns lazytest.test-utils
  (:require
   [lazytest.color :as lc]
   [matcher-combinators.core :as mc]
   [matcher-combinators.model :refer [->Mismatch]]
   [matcher-combinators.result :as result]))

(defmacro with-out-str-no-color
  "Evaluates exprs in a context in which *out* is bound to a fresh StringWriter and lazytest.color/*color* is bound to false.  Returns the string created by any nested printing calls or nil if the string is empty."
  {:added "1.0"}
  [& body]
  `(let [s# (java.io.StringWriter.)]
     (binding [*out* s#
               lc/*color* false]
       ~@body
       (not-empty (str s#)))))

(defmacro with-out-str-data-map
  "Adapted from clojure.core/with-out-str.

  Evaluates exprs in a context in which *out* is bound to a fresh
  StringWriter. Returns the result of the body and the string created by any
  nested printing calls in a map under the respective keys :result and :string."
  [& body]
  `(let [s# (java.io.StringWriter.)]
     (binding [*out* s#
               lc/*color* false]
       (let [r# (do ~@body)]
         {:result r#
          :string (not-empty (str s#))}))))

(defn Throwable->matchable [obj]
  (let [th (Throwable->map obj)]
    (-> th
        (assoc :type (-> th :via first :type))
        (dissoc :trace :via :at))))

(defn- prepare-expected-ex [this]
  (let [th (Throwable->matchable this)]
    (cond-> th
      true (dissoc :type)
      true (update :data dissoc :type)
      (:type (:data th)) (assoc :type (:type (:data th))))))

(defn- prepare-actual-ex [actual]
  (update (Throwable->matchable actual) :data dissoc :type))

(extend-protocol mc/Matcher
  clojure.lang.ExceptionInfo
  (-matcher-for
    ([this] this)
    ([this _] this))
  (-base-name [_] 'ex-info-match)
  (-match [this actual]
    (if (instance? Throwable actual)
      (mc/match (prepare-expected-ex this) (prepare-actual-ex actual))
      {::result/type :mismatch
       ::result/value (->Mismatch (prepare-expected-ex this) actual)
       ::result/weight 1})))
