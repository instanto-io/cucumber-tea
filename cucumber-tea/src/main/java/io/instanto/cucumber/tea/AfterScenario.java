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

/** Marks a no-argument method to run after each generated scenario, including failed scenarios. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface AfterScenario {}
