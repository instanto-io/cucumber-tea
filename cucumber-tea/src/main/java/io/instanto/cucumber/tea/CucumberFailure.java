/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea;

/** Adds feature and step context while preserving the original assertion or application failure. */
public final class CucumberFailure extends AssertionError {
  /** Creates a failure with generated-test context and the original cause. */
  public CucumberFailure(String context, Throwable cause) {
    super(context + ": " + cause.getMessage());
    initCause(cause);
  }
}
