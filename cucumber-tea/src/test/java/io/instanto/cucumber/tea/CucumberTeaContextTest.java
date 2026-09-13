/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class CucumberTeaContextTest {

  @Test
  public void contextIsAvailableOnlyInsideItsScenario() throws Throwable {
    CucumberContext context = CucumberContext.named("local", "value");

    CucumberTea.withContext(
        context,
        () -> {
          assertEquals(context, CucumberTea.context());
          assertEquals("value", CucumberTea.context(String.class));
        });

    assertThrows(IllegalStateException.class, CucumberTea::context);
  }

  @Test
  public void nestedContextRestoresItsParentAfterFailure() throws Throwable {
    CucumberContext outer = CucumberContext.named("outer", 1);
    CucumberContext inner = CucumberContext.named("inner", 2);

    CucumberTea.withContext(
        outer,
        () -> {
          assertThrows(
              IllegalStateException.class,
              () ->
                  CucumberTea.withContext(
                      inner,
                      () -> {
                        assertEquals(Integer.valueOf(2), CucumberTea.context(Integer.class));
                        throw new IllegalStateException("expected");
                      }));
          assertEquals(Integer.valueOf(1), CucumberTea.context(Integer.class));
        });
  }
}
