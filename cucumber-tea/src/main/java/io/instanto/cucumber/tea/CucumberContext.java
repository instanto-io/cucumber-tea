/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea;

import java.util.Objects;

/** One explicitly named execution context for a generated JVM scenario. */
public final class CucumberContext {
  private final String name;
  private final Object value;

  private CucumberContext(String name, Object value) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("A Cucumber context name must not be blank");
    }
    this.name = name;
    this.value = Objects.requireNonNull(value, "A Cucumber context value is required");
  }

  public static CucumberContext named(String name, Object value) {
    return new CucumberContext(name, value);
  }

  public String name() {
    return name;
  }

  public Object value() {
    return value;
  }

  public <T> T value(Class<T> type) {
    return Objects.requireNonNull(type, "type").cast(value);
  }

  @Override
  public String toString() {
    return name;
  }
}
