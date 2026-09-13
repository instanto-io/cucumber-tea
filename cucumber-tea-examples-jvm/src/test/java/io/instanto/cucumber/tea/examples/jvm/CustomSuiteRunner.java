/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.jvm;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/** Test runner proving that a generated suite can delegate to host infrastructure. */
public final class CustomSuiteRunner extends BlockJUnit4ClassRunner {
  private static boolean active;

  public CustomSuiteRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
    active = true;
  }

  static boolean isActive() {
    return active;
  }
}
