/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea;

/** Runs scenarios and adds feature context to failures from generated tests. */
public final class CucumberTea {
  private static final ThreadLocal<CucumberContext> CURRENT_CONTEXT = new ThreadLocal<>();

  private CucumberTea() {}

  /** A generated scenario, hook, or step body. */
  @FunctionalInterface
  public interface Body {
    void run() throws Throwable;
  }

  /** Runs a generated JVM scenario with its explicit context available to its step definitions. */
  public static void withContext(CucumberContext context, Body body) throws Throwable {
    CucumberContext previous = CURRENT_CONTEXT.get();
    CURRENT_CONTEXT.set(context);
    try {
      body.run();
    } finally {
      if (previous == null) {
        CURRENT_CONTEXT.remove();
      } else {
        CURRENT_CONTEXT.set(previous);
      }
    }
  }

  /** The context selected for the current generated JVM scenario. */
  public static CucumberContext context() {
    CucumberContext context = CURRENT_CONTEXT.get();
    if (context == null) {
      throw new IllegalStateException("No Cucumber execution context is active");
    }
    return context;
  }

  /** The current context value, checked against its expected type. */
  public static <T> T context(Class<T> type) {
    return context().value(type);
  }

  /** Runs a scenario body between its before and after hooks. */
  public static void scenario(
      String feature, String scenario, Body before, Body body, Body after) throws Throwable {
    Throwable failure = null;
    try {
      before.run();
      body.run();
    } catch (Throwable caught) {
      failure = caught;
    } finally {
      try {
        after.run();
      } catch (Throwable caught) {
        failure = combine(failure, caught);
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  /** Runs every after hook, preserving the first failure and suppressing later failures. */
  public static void afterHooks(Body... hooks) throws Throwable {
    Throwable failure = null;
    for (Body hook : hooks) {
      try {
        hook.run();
      } catch (Throwable caught) {
        failure = combine(failure, caught);
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  /** Runs one step and adds its feature, scenario, and source location to any failure. */
  public static void step(
      String feature,
      String scenario,
      String source,
      String keyword,
      String text,
      Body body)
      throws Throwable {
    try {
      body.run();
    } catch (Throwable failure) {
      throw new CucumberFailure(
          feature + " / " + scenario + "\n" + source + "\n" + keyword + " " + text,
          failure);
    }
  }

  /** Runs one hook and adds its feature, scenario, and hook kind to any failure. */
  public static void hook(String feature, String scenario, String hook, Body body)
      throws Throwable {
    try {
      body.run();
    } catch (Throwable failure) {
      throw new CucumberFailure(feature + " / " + scenario + "\n" + hook + " hook", failure);
    }
  }

  private static Throwable combine(Throwable failure, Throwable caught) {
    if (failure == null) {
      return caught;
    }
    if (failure != caught) {
      failure.addSuppressed(caught);
    }
    return failure;
  }
}
