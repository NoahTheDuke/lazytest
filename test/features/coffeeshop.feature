Feature: Coffee shop order fulfilment

  The coffee shop contains a counter where people can place orders.

  Background:
    Given the following price list
      | Matcha Latte | 4.00 |
      | Green Tea    | 3.50 |

  Scenario: Getting change
    When I order a Matcha Latte
    And pay with $5.00
    Then I get $1.00 back

  Scenario: Paying with exact change
    When I order a Matcha Latte
    And I order a Green Tea
    And pay with $7.50
    Then I get $0.00 back
