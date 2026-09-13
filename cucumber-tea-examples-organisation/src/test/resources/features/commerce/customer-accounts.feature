Feature: Customer accounts

  Background:
    Given an isolated integration scenario

  Scenario: register a customer
    When I register customer "Ada"
    Then customer "Ada" is available
