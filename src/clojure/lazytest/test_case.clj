(ns lazytest.test-case)

(set! *warn-on-reflection* true)

^:clj-reload/keep
(defrecord TestCase [type doc body context ns file line metadata])

(defn test-case? [obj]
  (instance? TestCase obj))

(defn test-case
  "A test case body may execute arbitrary code and may have side effects.
  It should throw an exception to indicate failure. Returning without
  throwing an exception indicates success."
  {:arglists '([{:keys [doc body context ns file line metadata] :as test-case}])}
  [base]
  (map->TestCase (-> base
                     (update :context #(or % {}))
                     (assoc :type :lazytest/test-case))))

(defn- stacktrace-file-and-line
  "Adapted from clojure.test"
  [stacktrace]
  (when (seq stacktrace)
    (let [^StackTraceElement s (first stacktrace)]
      {:file (.getFileName s)
       :line (.getLineNumber s)})))

(defn- extract-file-line-doc [source thrown]
  (let [thrown-data (ex-data thrown)
        caught (:caught thrown-data)
        caught-data (when (instance? Throwable caught)
                      (stacktrace-file-and-line
                        (.getStackTrace ^Throwable caught)))]
    {:line (or (:line caught-data) (:line thrown-data) (:line source))
     :file (or (:file caught-data) (:file thrown-data) (:file source))
     :doc (or (:doc caught-data) (:doc thrown-data) (:doc source))}))

(defn test-case-result
  "Creates a test case result map with keys :pass?, :source, and :thrown.

  pass? is true if the test case passed successfully, false otherwise.

  source is the test case object that returned this result.

  thrown is the exception (Throwable) thrown by a failing test case."
  ([type' source] (test-case-result type' source nil))
  ([type' source thrown]
   (let [{:keys [file line doc]} (extract-file-line-doc source thrown)
         {:keys [actual expected message caught evaluated]} (ex-data thrown)
         thrown' (or (when (instance? Throwable caught) caught) thrown)]
     (-> source
       (assoc :type type')
       (assoc :doc doc)
       (assoc :source source)
       (assoc :thrown thrown')
       (assoc :file (or file "NO_SOURCE_PATH"))
       (assoc :line line)
       (assoc :message (or message (ex-message thrown')))
       (assoc :expected expected)
       (assoc :actual (when (some? actual) actual))
       (assoc :evaluated evaluated)
       (vary-meta assoc :type ::test-case-result)))))

(defn test-case-result?
  "True if x is a test case result."
  [x]
  (and (map? x) (isa? (type x) ::test-case-result)))

(defn identifier [result]
  (or (:doc result) "Anonymous test case"))

(defn try-test-case
  "Executes a test case function. Catches all Throwables. Returns a
   map with the following key-value pairs:

     :source - the input function
     :pass?  - true if the function ran without throwing
     :thrown - the Throwable instance if thrown"
  [tc]
  (let [f (:body tc)]
    (try (f)
      (test-case-result :pass tc)
      (catch Throwable t
        (test-case-result :fail tc t)))))
