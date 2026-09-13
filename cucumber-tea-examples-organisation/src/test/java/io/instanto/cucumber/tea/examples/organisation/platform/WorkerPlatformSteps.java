/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.organisation.platform;

import static org.junit.Assert.assertNull;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Then;
import io.instanto.cucumber.tea.examples.organisation.support.HostGlobals;
import io.instanto.cucumber.tea.examples.organisation.support.OrganisedIntegrationSteps;

/** A separate suite boundary for Worker infrastructure. */
@CucumberSuite("features/platform/worker-health.feature")
public final class WorkerPlatformSteps extends OrganisedIntegrationSteps {

  @Then("the integration is executing in a Worker")
  public void integrationUsesWorkerHost() {
    assertNull(HostGlobals.typeOf("window"));
  }
}
