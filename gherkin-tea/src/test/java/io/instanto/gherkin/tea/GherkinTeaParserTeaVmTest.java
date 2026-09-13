/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class GherkinTeaParserTeaVmTest {

  @Test
  public void parsesAFeatureInABrowser() {
    String source =
        "Feature: Timesheets\n"
            + "  Scenario: Recording hours\n"
            + "    Given an employee \"A-17\"\n"
            + "    When 38 hours are recorded\n"
            + "    Then the timesheet totals 38\n";

    GherkinFeature feature =
        new GherkinTeaParser().parse("timesheets.feature", source.getBytes(StandardCharsets.UTF_8));

    assertEquals("Timesheets", feature.name());
    assertEquals(1, feature.scenarios().size());
    assertEquals(3, feature.scenarios().get(0).steps().size());
  }
}
