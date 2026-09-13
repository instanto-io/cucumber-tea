/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.jvm;

import io.instanto.cucumber.tea.CucumberContext;
import io.instanto.cucumber.tea.CucumberContexts;
import java.util.List;

/** Explicit implementations against which every scenario in the suite is generated. */
public final class CalculatorContexts implements CucumberContexts {
  @Override
  public List<CucumberContext> contexts() {
    return List.of(
        CucumberContext.named(
            "direct JVM implementation", (Calculator) (left, right) -> left + right),
        CucumberContext.named(
            "method-reference JVM implementation", (Calculator) Math::addExact));
  }
}
