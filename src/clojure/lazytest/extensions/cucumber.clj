(ns lazytest.extensions.cucumber
  "Cucumber support through lambdaisland's excellent kaocha-cucumber library. Docstrings are taken from the Cucumber documentation.

  To see more details, read the official [Cucumber documentation](https://cucumber.io/docs/gherkin/reference)."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [lambdaisland.cucumber.dsl :as dsl]
   [lambdaisland.cucumber.gherkin :as gherkin]
   [lambdaisland.cucumber.jvm :as jvm]
   [lazytest.cli :refer [update-vec]]
   [lazytest.hooks :refer [defhook]]
   [lazytest.suite :as suite]
   [lazytest.test-case :as test-case])
  (:import
   [cucumber.api PendingException]
   [cucumber.api.event SnippetsSuggestedEvent TestCaseFinished]
   [cucumber.runtime.io Resource]
   [gherkin.pickles PickleLocation]
   [java.io File]))

(set! *warn-on-reflection* true)

(defmacro Given
  "Given steps are used to describe the initial context of the system - the scene of the scenario. It is typically something that happened in te past.

  When Cucumber executes a Given step, it will configure the system to be in a well-defined state, such as creating and configuring objects or adding data to a test database.

  The purpose of Given steps is to put the system in a known state before the user (or external system) starts interacting with the system (in the When steps). Avoid talking about user interaction in Given’s. If you were creating use cases, Given’s would be your preconditions.

  It’s okay to have several Given steps (use And or But for number 2 and upwards to make it more readable).

  Examples:

      Mickey and Minnie have started a game
      I am logged in
      Joe has a balance of £42"
  [pattern binds & body]
  (with-meta `(dsl/Given ~pattern ~binds ~@body)
    (meta &form)))

(defmacro When
  "When steps are used to describe an event, or an action. This can be a person interacting with the system, or it can be an event triggered by another system.

  Examples:

      Guess a word
      Invite a friend
      Withdraw money"
  [pattern binds & body]
  (with-meta `(dsl/When ~pattern ~binds ~@body)
    (meta &form)))

(defmacro Then
  "Then steps are used to describe an expected outcome, or result.

  The step definition of a Then step should use an assertion to compare the actual outcome (what the system actually does) to the expected outcome (what the step says the system is supposed to do).

  An outcome should be on an observable output. That is, something that comes out of the system (report, user interface, message), and not a behaviour deeply buried inside the system (like a record in a database).

  Examples:

      See that the guessed word was wrong
      Receive an invitation
      Card should be swallowed

  While it might be tempting to implement Then steps to look in the database - resist that temptation!

  You should only verify an outcome that is observable for the user (or external system), and changes to a database are usually not."
  [pattern binds & body]
  (with-meta `(dsl/Then ~pattern ~binds ~@body)
    (meta &form)))

(defmacro And
  "An alias for Given, allowing for more fluidly structured examples.

  Example: Multiple Givens
    Given one thing
    And another thing
    And yet another thing
    When I open my eyes
    Then I should see something
    But I shouldn't see something else"
  [pattern binds & body]
  (with-meta `(dsl/And ~pattern ~binds ~@body)
    (meta &form)))

(defmacro But
  "An alias for Given, allowing for more fluidly structured examples.

  Example: Multiple Givens
    Given one thing
    And another thing
    And yet another thing
    When I open my eyes
    Then I should see something
    But I shouldn't see something else"
  [pattern binds & body]
  (with-meta `(dsl/But ~pattern ~binds ~@body)
    (meta &form)))

(defmacro Before
  [pattern binds & body]
  (with-meta `(dsl/Before ~pattern ~binds ~@body)
    (meta &form)))

(defmacro After
  [pattern binds & body]
  (with-meta `(dsl/After ~pattern ~binds ~@body)
    (meta &form)))

(defn pending!
  "Throws "
  []
  (throw (PendingException.)))

(defn handle-event-dispatch [_state event] (jvm/event->type event))

(defmulti handle-event {:arglists '([state event])} #'handle-event-dispatch)
(defmethod handle-event :default handle-event--default [_state _event])

(defmethod handle-event :cucumber/test-run-started
  handle-event--cucumber-test-run-started [state _event] state)
(defmethod handle-event :cucumber/test-run-finished
  handle-event--cucumber-test-run-finished [state _event] state)
(defmethod handle-event :cucumber/test-source-read
  handle-event--cucumber-test-source-read [state _event] state)
(defmethod handle-event :cucumber/test-case-started
  handle-event--cucumber-test-case-started [state _event] state)
(defmethod handle-event :cucumber/test-step-started
  handle-event--cucumber-test-step-started [state _event] state)
(defmethod handle-event :cucumber/test-step-finished
  handle-event--cucumber-test-step-finished [state _event] state)

(defmethod handle-event :cucumber/snippets-suggested-event
  handle-event--cucumber-snippets-suggested-event
  [state ^SnippetsSuggestedEvent event]
  (let [snippet {:snippets (.-snippets event)
                 :lines (mapv #(.getLine ^PickleLocation %) (.-stepLocations event))}]
    (update state ::snippets #(conj (or % []) snippet))))

(defmethod handle-event :cucumber/test-case-finished
  handle-event--cucumber-test-case-finished
  [state ^TestCaseFinished event]
  (let [{:keys [error]} (jvm/result->edn (.-result event))]
    (if error
      (throw error)
      state)))

(defn- tags->metadata [obj]
  (->> (:tags obj)
    (keep :name)
    (map (fn [tag] [(keyword (subs tag 1)) true]))
    (into {})
    (not-empty)))

(defn scenario->test-case [ctx feature]
  (let [scenario (first (gherkin/scenarios feature))
        body (fn []
               (jvm/execute! {:features [(gherkin/edn->gherkin feature)]
                              :state (:lazytest.cucumber/state ctx)
                              :glue (:lazytest.cucumber/step-paths ctx)
                              :handler handle-event}))
        tags (tags->metadata scenario)]
    (test-case/test-case {:doc (str "Scenario: " (:name scenario))
                          :body body
                          :metadata tags})))

(defn gherkin-dispatch [_ctx obj] (:type obj))

(defmulti gherkin->suite {:arglists '([ctx obj])} #'gherkin-dispatch)

(defmethod gherkin->suite :cucumber/feature
  gherkin->suite--cucumber-feature
  [ctx obj]
  (let [uri (:uri obj)
        ctx (-> ctx
                (assoc ::uri uri)
                (assoc ::feature obj))]
    (gherkin->suite ctx (:document obj))))

(defn print-snippets
  [ctx snippets]
  (when (seq snippets)
    (let [steps (->> (:children (:feature (:document (::feature ctx))))
                     (mapcat :steps)
                     (map (juxt #(:line (:location %)) identity))
                     (into {}))
          strs (for [{:keys [snippets lines]} snippets
                     [snippet line] (map vector snippets lines)
                     :let [step (get steps line)]
                     :when step]
                 (str/replace snippet "**KEYWORD** " (:keyword step)))]
      (when (seq strs)
        (newline)
        (println "Cucumber steps that need to be implemented:")
        (newline)
        (print (str/join "\n" strs))
        (flush)))))

(defmethod gherkin->suite :gherkin/document
  gherkin->suite--gherkin-document
  [ctx obj]
  (gherkin->suite ctx (:feature obj)))

(defmethod gherkin->suite :gherkin/feature
  gherkin->suite--gherkin-feature
  [ctx obj]
  (let [state (:lazytest.cucumber/state ctx)
        line (:line (:location obj))
        children (mapv #(scenario->test-case ctx %) (gherkin/dedupe-feature (::feature ctx)))
        tags (tags->metadata obj)]
    (suite/suite {:doc (str "Feature: " (:name obj))
                  :file (::uri ctx)
                  :line line
                  :children children
                  :context {:after [#(print-snippets ctx (::snippets @state))]}
                  :metadata tags})))

(defn load-cucumber-features
  [ctx]
  (->> (:lazytest.cucumber/feature-paths ctx)
    (into []
      (comp
        (map io/file)
        (mapcat file-seq)
        (keep #(when (.isFile ^File %) (str %)))
        (mapcat jvm/find-features)
        (keep (fn [^Resource resource]
                (let [path (.getPath resource)]
                  (try
                    (gherkin/gherkin->edn (jvm/parse-resource resource))
                    (catch Throwable e
                      (println "Failed loading " path ": " (ex-message e))
                      nil)))))))
    (not-empty)))

(defn features->suite
  [ctx features]
  (let [suites (mapv #(gherkin->suite ctx %) features)]
    (suite/suite {:doc "Cucumber Tests"
                  :children suites})))

(defhook hook
  "Run cucumber tests"
  (cli-opts [_config opts]
    (into opts
      [[nil "--[no-]cucumber" "Enable or disable cucumber loading and runs (Defaults to true)"
        :id :lazytest.cucumber/enabled
        :default true]
       [nil "--cucumber-features DIR" "Directory containing .feature files. (Defaults to --dir or \"test\")"
        :id :lazytest.cucumber/feature-paths
        :assoc-fn update-vec]
       [nil "--cucumber-steps DIR" "Directory containing .clj step definitions. (Required)"
        :id :lazytest.cucumber/step-paths
        :assoc-fn update-vec]]))
  (config [config _]
    (when (:lazytest.cucumber/enabled config)
      (when-not (seq (:lazytest.cucumber/step-paths config))
        (throw (ex-info "cucumber hook: Must provide --cucumber-steps" {})))
      (-> config
        (update :lazytest.cucumber/feature-paths #(or (not-empty %) (:dirs config))))))
  (pre-test-run [config suite]
    (when (:lazytest.cucumber/enabled config)
      (let [ctx (-> config
                  (select-keys [:lazytest.cucumber/feature-paths
                                :lazytest.cucumber/step-paths])
                  (assoc :lazytest.cucumber/state (atom {})))]
        (when-let [features (load-cucumber-features ctx)]
          (update suite :children conj (features->suite ctx features)))))))
