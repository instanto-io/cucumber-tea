/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

/** A Gherkin doc string after Scenario Outline substitution. */
public record GherkinDocString(String content, String mediaType) implements GherkinStepArgument {}
