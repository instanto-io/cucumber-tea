/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.examples.organisation.support;

import org.teavm.jso.JSBody;

/** Keeps the host probe out of domain step classes. */
public final class HostGlobals {
  private HostGlobals() {}

  @JSBody(params = "name", script = "return name in globalThis ? typeof globalThis[name] : null;")
  public static native String typeOf(String name);
}
