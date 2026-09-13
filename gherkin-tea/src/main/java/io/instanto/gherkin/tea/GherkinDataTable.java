/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

import java.util.ArrayList;
import java.util.List;

/** A Gherkin data table after Scenario Outline substitution. */
public record GherkinDataTable(List<List<String>> rows) implements GherkinStepArgument {
  public GherkinDataTable {
    List<List<String>> copied = new ArrayList<>();
    for (List<String> row : rows) {
      copied.add(List.copyOf(row));
    }
    rows = List.copyOf(copied);
  }
}
