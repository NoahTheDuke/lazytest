(ns lazytest.test-case
  (:import
    (lazytest ExpectationFailed)))

(defn test-case
  "Sets metadata on function f identifying it as a test case. A test
  case function may execute arbitrary code and may have side effects.
  It should throw an exception to indicate failure. Returning without
  throwing an exception indicates success.

  Additional identifying metadata may be placed on the function, such
  as :ns-name and :doc."
  [f]
  {:pre [(fn? f)]}
  (vary-meta f assoc ::test-case true :type :lazytest/test-case))

(defn test-case?
  "True if x is a test case."
  [x]
  (and (fn? x) (::test-case (meta x))))

(defn- extract-file-and-line [source thrown]
  (let [m (or (ex-data thrown) (meta source))]
    (select-keys m [:line :file])))

(defn test-case-result
  "Creates a test case result map with keys :pass?, :source, and :thrown.

  pass? is true if the test case passed successfully, false otherwise.

  source is the test case object that returned this result.

  thrown is the exception (Throwable) thrown by a failing test case."
  ([type' source] (test-case-result type' source nil))
  ([type' source thrown]
   {:pre [(test-case? source)
          (or (nil? thrown) (instance? Throwable thrown))]}
   (let [{:keys [file line]} (extract-file-and-line source thrown)]
     (with-meta {:type type' :source source :thrown thrown
                 :file file :line line}
       {:type ::test-case-result}))))

(defn test-case-result?
  "True if x is a test case result."
  [x]
  (and (map? x) (isa? (type x) ::test-case-result)))

(defn try-test-case
  "Executes a test case function. Catches all Throwables. Returns a
   map with the following key-value pairs:

     :source - the input function
     :pass?  - true if the function ran without throwing
     :thrown - the Throwable instance if thrown"
  [f]
  {:pre [(test-case? f)]
   :post [(test-case-result? %)]}
  (try (f)
    (test-case-result :pass f)
    (catch ExpectationFailed ex
      (test-case-result :fail f ex))
    (catch Throwable t
      (test-case-result :error f t))))
