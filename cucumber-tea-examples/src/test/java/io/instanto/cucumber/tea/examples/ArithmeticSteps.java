/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.instanto.cucumber.tea.AfterScenario;
import io.instanto.cucumber.tea.BeforeScenario;
import io.instanto.cucumber.tea.CucumberScript;
import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.DataTable;
import io.instanto.cucumber.tea.Given;
import io.instanto.cucumber.tea.Then;
import io.instanto.cucumber.tea.When;
import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSBody;

@CucumberSuite(
    value = "features/arithmetic.feature",
    scripts = @CucumberScript(resource = "cucumber-tea-example-helper.js", path = "cucumber-tea-example-helper.js"))
public class ArithmeticSteps {
  private final List<String> history = new ArrayList<>();
  private int display;
  private boolean beforeRan;

  @BeforeScenario
  void beforeScenario() {
    beforeRan = true;
  }

  @AfterScenario
  void afterScenario() {
    assertTrue("The before hook should run in the same target runtime", beforeRan);
  }

  @AfterScenario
  void clearHistory() {
    history.clear();
  }

  @Given("a fresh calculator")
  void freshCalculator() {
    assertTrue("Before hooks run before Background steps", beforeRan);
    display = 0;
    history.clear();
  }

  @Given("the display shows {int}")
  void displayShows(int value) {
    display = value;
    history.add("start:" + value);
  }

  @When("I add {int}")
  void add(int value) {
    display += value;
    history.add("add:" + value);
  }

  @Then("the result is {int}")
  void resultIs(int expected) {
    assertEquals(expected, display);
  }

  @Then("the history is")
  void historyIs(DataTable expected) {
    assertEquals(3, expected.height());
    assertEquals("start", expected.cell(1, 0));
    assertEquals("4", expected.cell(1, 1));
    assertEquals("add", expected.cell(2, 0));
    assertEquals("3", expected.cell(2, 1));
    assertEquals(List.of("start:4", "add:3"), history);
  }

  @Then("the report is")
  void reportIs(String expected) {
    assertEquals("start=4\nadd=3\nresult=7", expected);
  }

  @Then("the browser global is available")
  void browserGlobalIsAvailable() {
    assertEquals("object", globalType("window"));
    assertEquals(42, browserHelperValue());
  }

  @Then("browser globals are absent")
  void browserGlobalsAreAbsent() {
    assertEquals(null, globalType("window"));
  }

  @JSBody(params = "name", script = "return name in globalThis ? typeof globalThis[name] : null;")
  private static native String globalType(String name);

  @JSBody(script = "return globalThis.cucumberTeaExampleHelper();")
  private static native int browserHelperValue();
}
