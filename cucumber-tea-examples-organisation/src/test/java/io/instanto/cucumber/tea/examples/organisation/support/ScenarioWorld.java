/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.organisation.support;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Mutable integration state with exactly one owner: the suite instance for a generated scenario. */
public final class ScenarioWorld {
  private final Set<String> customers = new LinkedHashSet<>();
  private final Map<String, Integer> orderQuantities = new LinkedHashMap<>();
  private boolean active;
  private boolean isolated;

  public void begin() {
    if (active) {
      throw new IllegalStateException("The scenario world is already active");
    }
    active = true;
  }

  public void isolate() {
    requireActive();
    if (isolated) {
      throw new IllegalStateException("The scenario world was isolated twice");
    }
    customers.clear();
    orderQuantities.clear();
    isolated = true;
  }

  public void finish() {
    requireActive();
    if (!isolated) {
      throw new IllegalStateException("The scenario did not establish its isolated world");
    }
    active = false;
  }

  public void registerCustomer(String name) {
    requireIsolated();
    customers.add(name);
  }

  public boolean hasCustomer(String name) {
    requireIsolated();
    return customers.contains(name);
  }

  public void placeOrder(String customer, int quantity) {
    requireIsolated();
    if (!customers.contains(customer)) {
      throw new IllegalStateException("Unknown customer " + customer);
    }
    orderQuantities.put(customer, quantity);
  }

  public int orderQuantity(String customer) {
    requireIsolated();
    Integer quantity = orderQuantities.get(customer);
    if (quantity == null) {
      throw new IllegalStateException("No order for " + customer);
    }
    return quantity;
  }

  private void requireActive() {
    if (!active) {
      throw new IllegalStateException("The scenario world is not active");
    }
  }

  private void requireIsolated() {
    requireActive();
    if (!isolated) {
      throw new IllegalStateException("The scenario world is not isolated");
    }
  }
}
