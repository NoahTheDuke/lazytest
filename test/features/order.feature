Feature: Steps are executed one by one

  @first
  Scenario: Executed step by step
    Given I have a foo fixture with value "foo"
    And there is a list
    When I append 1 to the list
    And I append 2 to the list
    And I append 3 to the list
    Then foo should have value "foo"
    But the list should be [1 2 3]

  @second
  Scenario: Another step by step
    Given I have a bar fixture with value "bellows"
    And there is a list
    When I append 5 to the list
    And I append 6 to the list
    And I append 7 to the list
    And I append 8 to the list
    Then bar should have value "bellows"
    But the list should be [5 6 8 9]
