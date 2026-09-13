/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

/** A doc string or data table attached to a feature step. */
public sealed interface GherkinStepArgument
    permits GherkinDocString, GherkinDataTable {}
