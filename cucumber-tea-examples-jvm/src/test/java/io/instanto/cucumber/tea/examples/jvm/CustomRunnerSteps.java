/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.jvm;

import static org.junit.Assert.assertTrue;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Then;

@CucumberSuite(
    value = "features/custom-runner.feature",
    runner = CustomSuiteRunner.class)
public final class CustomRunnerSteps {

  @Then("the selected host runner is active")
  public void selectedHostRunnerIsActive() {
    assertTrue(CustomSuiteRunner.isActive());
  }
}
