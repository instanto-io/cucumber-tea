/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

/** One expanded step and its optional multiline argument. */
public record GherkinStep(
    String keyword, String text, int line, GherkinStepArgument argument) {}
