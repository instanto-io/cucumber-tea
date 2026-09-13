/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.codegen;

import java.util.ArrayList;
import java.util.List;

/** Compile-time selection policy for Cucumber Tea's reserved runtime tags. */
final class TargetTags {
  static final String SKIP_JVM = "@skip-jvm";
  static final String BROWSER = "@browser";
  static final String WORKER = "@worker";
  static final String JVM = "@jvm";
  static final String PORTABLE = "@portable";

  private TargetTags() {}

  enum Runtime {
    JVM,
    BROWSER,
    WORKER
  }

  static String conflict(List<String> tags) {
    if (tags.contains(JVM) && tags.contains(SKIP_JVM)) {
      return "conflicting Cucumber Tea target tags [@jvm, @skip-jvm]; "
          + "a JVM-only scenario cannot skip JVM execution";
    }
    List<String> selected = new ArrayList<>();
    if (tags.contains(BROWSER)) selected.add(BROWSER);
    if (tags.contains(WORKER)) selected.add(WORKER);
    if (tags.contains(JVM)) selected.add(JVM);
    if (tags.contains(PORTABLE)) selected.add(PORTABLE);
    if (selected.size() < 2) {
      return null;
    }
    return "conflicting Cucumber Tea target tags " + selected
        + "; use exactly one of @jvm, @browser, @worker, or @portable";
  }

  static boolean includes(List<String> tags, Runtime runtime) {
    if (runtime == Runtime.JVM && tags.contains(SKIP_JVM)) {
      return false;
    }
    if (tags.contains(PORTABLE)) {
      return true;
    }
    if (tags.contains(BROWSER)) {
      return runtime == Runtime.BROWSER;
    }
    if (tags.contains(WORKER)) {
      return runtime == Runtime.WORKER;
    }
    if (tags.contains(JVM)) {
      return runtime == Runtime.JVM;
    }
    return true;
  }
}
