/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.organisation.support;

import io.instanto.cucumber.tea.AfterScenario;
import io.instanto.cucumber.tea.BeforeScenario;
import io.instanto.cucumber.tea.Given;

/** Lifecycle and vocabulary shared by independent integration suites. */
public abstract class OrganisedIntegrationSteps {
  protected final ScenarioWorld world = new ScenarioWorld();

  @BeforeScenario
  public void beginScenario() {
    world.begin();
  }

  @Given("an isolated integration scenario")
  public void isolateScenario() {
    world.isolate();
  }

  @AfterScenario
  public void finishScenario() {
    world.finish();
  }
}
