Feature: Contextual arithmetic

  Scenario: add a value
    Given the display shows 40
    When I add 2
    Then the result is 42
