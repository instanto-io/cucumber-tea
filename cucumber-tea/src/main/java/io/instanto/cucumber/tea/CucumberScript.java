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

/** One JavaScript resource required by a generated browser test suite. */
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface CucumberScript {
  /** Classpath resource copied into TeaVM's test output. */
  String resource();

  /** File name used in TeaVM's generated browser-test output. */
  String path();
}
