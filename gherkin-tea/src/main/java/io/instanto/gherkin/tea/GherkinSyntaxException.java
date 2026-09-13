/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

/** Raised at build time when the official Gherkin parser rejects a feature. */
public final class GherkinSyntaxException extends RuntimeException {
  public GherkinSyntaxException(String message) {
    super(message);
  }
}
