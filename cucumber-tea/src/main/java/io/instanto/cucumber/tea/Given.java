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

/** Defines a step using a Cucumber Expression. Keywords do not affect matching. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Given {
  /** The Cucumber Expression matched against feature steps. */
  String value();
}
