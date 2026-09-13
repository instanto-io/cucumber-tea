Feature: Arithmetic
  A feature is parsed once at build time and runs unchanged in either JavaScript host.

  Background:
    Given a fresh calculator

  @portable @outline
  Scenario Outline: add a value
    Given the display shows <start>
    When I add <amount>
    Then the result is <result>

    Examples:
      | start | amount | result |
      | 40    | 2      | 42     |
      | 10    | 5      | 15     |

  @portable
  Scenario: carry structured feature arguments into Java
    Given the display shows 4
    When I add 3
    Then the history is
      | operation | value |
      | start     | 4     |
      | add       | 3     |
    And the report is
      """
      start=4
      add=3
      result=7
      """

  @browser @skip-jvm
  Scenario: execute browser integration in Chromium
    Then the browser global is available

  @worker @skip-jvm
  Scenario: execute Worker integration in workerd
    Then browser globals are absent
