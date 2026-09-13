/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

import java.util.List;

/** A standard Gherkin feature after backgrounds and outlines have been expanded. */
public record GherkinFeature(String uri, String name, List<GherkinScenario> scenarios) {
  public GherkinFeature {
    scenarios = List.copyOf(scenarios);
  }
}
