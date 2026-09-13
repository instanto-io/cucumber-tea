/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.jvm;

/** Behaviour shared by the example's explicitly selected test contexts. */
interface Calculator {
  int add(int left, int right);
}
