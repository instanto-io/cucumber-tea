/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.jvm;

import static org.junit.Assert.assertEquals;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.CucumberTea;
import io.instanto.cucumber.tea.Given;
import io.instanto.cucumber.tea.Then;
import io.instanto.cucumber.tea.When;

@CucumberSuite(
    value = "features/contextual-arithmetic.feature",
    contexts = CalculatorContexts.class)
public final class ContextualArithmeticSteps {
  private int display;

  @Given("the display shows {int}")
  public void displayShows(int value) {
    display = value;
  }

  @When("I add {int}")
  public void add(int value) {
    display = CucumberTea.context(Calculator.class).add(display, value);
  }

  @Then("the result is {int}")
  public void resultIs(int expected) {
    assertEquals(expected, display);
  }
}
