/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.organisation.commerce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Given;
import io.instanto.cucumber.tea.Then;
import io.instanto.cucumber.tea.When;
import io.instanto.cucumber.tea.examples.organisation.support.HostGlobals;
import io.instanto.cucumber.tea.examples.organisation.support.OrganisedIntegrationSteps;

/** One suite boundary for related customer and ordering capabilities. */
@CucumberSuite({
  "features/commerce/customer-accounts.feature",
  "features/commerce/order-placement.feature"
})
public final class CommerceIntegrationSteps extends OrganisedIntegrationSteps {

  @When("I register customer {string}")
  public void registerCustomer(String name) {
    world.registerCustomer(name);
  }

  @Given("a registered customer {string}")
  public void registeredCustomer(String name) {
    world.registerCustomer(name);
  }

  @Then("customer {string} is available")
  public void customerIsAvailable(String name) {
    assertTrue(world.hasCustomer(name));
  }

  @When("{string} places an order for {int} items")
  public void placeOrder(String customer, int quantity) {
    world.placeOrder(customer, quantity);
  }

  @Then("the order for {string} contains {int} items")
  public void orderContains(String customer, int quantity) {
    assertEquals(quantity, world.orderQuantity(customer));
  }

  @Then("checkout is executing in a browser")
  public void checkoutUsesBrowserHost() {
    assertEquals("object", HostGlobals.typeOf("window"));
  }
}
