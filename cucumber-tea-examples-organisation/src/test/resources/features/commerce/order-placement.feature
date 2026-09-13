Feature: Order placement

  Background:
    Given an isolated integration scenario

  @portable
  Scenario Outline: retain the order quantity
    Given a registered customer "<customer>"
    When "<customer>" places an order for <quantity> items
    Then the order for "<customer>" contains <quantity> items

    Examples:
      | customer | quantity |
      | Ada      | 2        |
      | Grace    | 5        |

  @browser @skip-jvm
  Scenario: render checkout in its browser host
    Then checkout is executing in a browser
