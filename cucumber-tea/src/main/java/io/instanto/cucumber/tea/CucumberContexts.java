/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea;

import java.util.List;

/** Supplies the explicit named contexts over which a JVM suite is executed. */
@FunctionalInterface
public interface CucumberContexts {
  List<CucumberContext> contexts();

  /** Annotation default representing a suite with no context matrix. */
  final class None implements CucumberContexts {
    @Override
    public List<CucumberContext> contexts() {
      return List.of();
    }
  }
}
