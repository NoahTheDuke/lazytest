(ns lazytest.test-case
  (:require
    [malli.experimental :as mx])
  (:import
    (lazytest ExpectationFailed)))

(defmacro test-case
  "Sets metadata on function f identifying it as a test case. A test
  case function may execute arbitrary code and may have side effects.
  It should throw an exception to indicate failure. Returning without
  throwing an exception indicates success.

  Additional identifying metadata may be placed on the function, such
  as :ns-name and :doc."
  [f]
  `(let [f# ~f
         fm# (meta f#)]
     (vary-meta f# assoc
                :type :lazytest/test-case
                :file ~*file*
                :line (or (:line fm#) ~(:line (meta &form)))
                ::test-case true)))

(defn test-case?
  "True if x is a test case."
  [x]
  (and (fn? x) (::test-case (meta x))))

(defn- stacktrace-file-and-line
  "Adapted from clojure.test"
  [stacktrace]
  (when (seq stacktrace)
    (let [^StackTraceElement s (first stacktrace)]
      {:file (.getFileName s)
       :line (.getLineNumber s)})))

(defn- extract-file-line-doc [source thrown]
  (let [thrown-data (ex-data thrown)
        caught-data (when (instance? Throwable (:caught thrown-data))
                      (stacktrace-file-and-line
                        (.getStackTrace ^Throwable (:caught thrown-data))))
        source-meta (meta source)
        m (merge source-meta thrown-data caught-data)]
    (select-keys m [:line :file :doc])))

(mx/defn test-case-result
  "Creates a test case result map with keys :pass?, :source, and :thrown.

  pass? is true if the test case passed successfully, false otherwise.

  source is the test case object that returned this result.

  thrown is the exception (Throwable) thrown by a failing test case."
  ([type' source] (test-case-result type' source nil))
  ([type'
    source :- [:fn test-case?]
    thrown :- [:maybe :lt/throwable]]
   (let [{:keys [file line doc]} (extract-file-line-doc source thrown)
         {:keys [actual expected caught evaluated]} (ex-data thrown)
         thrown' (or (when (instance? Throwable caught) caught) thrown)]
     (with-meta {:type type'
                 :source source :thrown thrown'
                 :file (or file "NO_SOURCE_PATH") :line line
                 :doc doc
                 :message (or (:message (ex-data thrown'))
                              (:expected-message (ex-data thrown')))
                 :expected expected
                 :actual (or actual caught)
                 :evaluated evaluated}
                {:type ::test-case-result}))))

(defn test-case-result?
  "True if x is a test case result."
  [x]
  (and (map? x) (isa? (type x) ::test-case-result)))

(defn identifier [result]
  (or (:doc result) "Anonymous test case"))

(mx/defn try-test-case
  "Executes a test case function. Catches all Throwables. Returns a
   map with the following key-value pairs:

     :source - the input function
     :pass?  - true if the function ran without throwing
     :thrown - the Throwable instance if thrown"
  [f :- [:fn test-case?]]
  (try (f)
    (test-case-result :pass f)
    (catch ExpectationFailed ex
      (if (:caught (ex-data ex))
        (test-case-result :error f ex)
        (test-case-result :fail f ex)))
    (catch Throwable t
      (test-case-result :error f t))))
