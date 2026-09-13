Feature: Worker health

  Background:
    Given an isolated integration scenario

  @worker @skip-jvm
  Scenario: execute a platform probe in workerd
    Then the integration is executing in a Worker
