/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Associates one step-definition class with one or more classpath feature resources. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface CucumberSuite {
  /** Classpath paths of the feature resources in this suite. */
  String[] value();

  /** Classpath JavaScript resources copied beside and loaded before the generated TeaVM test. */
  CucumberScript[] scripts() default {};

  /**
   * Explicit execution contexts for a JVM suite. Each generated scenario runs once for every
   * named context returned by the provider.
   */
  Class<? extends CucumberContexts> contexts() default CucumberContexts.None.class;

  /**
   * Optional JUnit runner for the generated suite.
   *
   * <p>The selected Cucumber Tea target still controls scenario tags and generated browser or
   * worker support. This setting replaces only the target's standard runner, allowing a host-aware
   * runner to start external infrastructure before delegating to TeaVM or another runtime.
   */
  Class<?> runner() default DefaultRunner.class;

  /** Marker used when the generated suite should use its target's standard runner. */
  final class DefaultRunner {
    private DefaultRunner() {}
  }
}
