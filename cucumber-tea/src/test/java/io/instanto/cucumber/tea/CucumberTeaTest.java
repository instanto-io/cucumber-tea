/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class CucumberTeaTest {
  @Test
  public void scenarioRunsHooksAroundTheBody() throws Throwable {
    List<String> calls = new ArrayList<>();

    CucumberTea.scenario(
        "feature",
        "scenario",
        () -> calls.add("before"),
        () -> calls.add("body"),
        () -> calls.add("after"));

    assertEquals(List.of("before", "body", "after"), calls);
  }

  @Test
  public void afterHookRunsWhenTheBodyFails() {
    RuntimeException expected = new RuntimeException("broken");
    List<String> calls = new ArrayList<>();

    try {
      CucumberTea.scenario(
          "feature", "scenario", () -> {}, () -> { throw expected; }, () -> calls.add("after"));
      fail("Expected failure");
    } catch (Throwable actual) {
      assertSame(expected, actual);
    }
    assertEquals(List.of("after"), calls);
  }

  @Test
  public void allAfterHooksRunAndFailuresAreAggregated() {
    RuntimeException first = new RuntimeException("first");
    RuntimeException second = new RuntimeException("second");
    List<String> calls = new ArrayList<>();

    try {
      CucumberTea.afterHooks(
          () -> {
            calls.add("first");
            throw first;
          },
          () -> {
            calls.add("second");
            throw second;
          },
          () -> calls.add("third"));
      fail("Expected failure");
    } catch (Throwable actual) {
      assertSame(first, actual);
      assertEquals(1, actual.getSuppressed().length);
      assertSame(second, actual.getSuppressed()[0]);
    }
    assertEquals(List.of("first", "second", "third"), calls);
  }

  @Test
  public void repeatedFailureInstanceDoesNotMaskTheOriginalFailure() {
    RuntimeException expected = new RuntimeException("shared");

    try {
      CucumberTea.scenario(
          "feature",
          "scenario",
          () -> {},
          () -> {
            throw expected;
          },
          () -> {
            throw expected;
          });
      fail("Expected failure");
    } catch (Throwable actual) {
      assertSame(expected, actual);
      assertEquals(0, actual.getSuppressed().length);
    }
  }

  @Test
  public void stepFailuresRetainTheCauseAndFeatureLocation() {
    AssertionError expected = new AssertionError("expected 42");

    try {
      CucumberTea.step(
          "Arithmetic",
          "addition",
          "features/arithmetic.feature:12",
          "Then",
          "the result is 42",
          () -> { throw expected; });
      fail("Expected failure");
    } catch (CucumberFailure actual) {
      assertSame(expected, actual.getCause());
      assertTrue(actual.getMessage().contains("Arithmetic / addition"));
      assertTrue(actual.getMessage().contains("features/arithmetic.feature:12"));
      assertTrue(actual.getMessage().contains("Then the result is 42"));
    } catch (Throwable unexpected) {
      throw new AssertionError(unexpected);
    }
  }
}
