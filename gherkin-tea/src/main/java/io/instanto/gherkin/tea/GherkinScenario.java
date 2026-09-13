/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

import java.util.List;

/** One executable scenario (a Gherkin pickle). */
public record GherkinScenario(String name, int line, List<String> tags, List<GherkinStep> steps) {
  public GherkinScenario {
    tags = List.copyOf(tags);
    steps = List.copyOf(steps);
  }
}
