/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.codegen;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class TargetTagsTest {
  @Test
  public void untaggedAndPortableScenariosRunInBothTargets() {
    assertIncludedInBoth(List.of());
    assertIncludedInBoth(List.of("@portable"));
    assertIncludedInBoth(List.of("@slow", "@portable"));
  }

  @Test
  public void browserScenariosRunOnlyInTheBrowser() {
    assertTrue(TargetTags.includes(List.of("@browser"), TargetTags.Runtime.BROWSER));
    assertFalse(TargetTags.includes(List.of("@browser"), TargetTags.Runtime.WORKER));
  }

  @Test
  public void workerScenariosRunOnlyInTheWorker() {
    assertFalse(TargetTags.includes(List.of("@worker"), TargetTags.Runtime.BROWSER));
    assertTrue(TargetTags.includes(List.of("@worker"), TargetTags.Runtime.WORKER));
  }

  @Test
  public void jvmScenariosRunOnlyInTheJvm() {
    assertTrue(TargetTags.includes(List.of("@jvm"), TargetTags.Runtime.JVM));
    assertFalse(TargetTags.includes(List.of("@jvm"), TargetTags.Runtime.BROWSER));
    assertFalse(TargetTags.includes(List.of("@jvm"), TargetTags.Runtime.WORKER));
  }

  @Test
  public void skipJvmScenariosAreNotGeneratedForTheJvmTarget() {
    assertFalse(TargetTags.includes(List.of("@skip-jvm"), TargetTags.Runtime.JVM));
    assertTrue(TargetTags.includes(List.of("@skip-jvm"), TargetTags.Runtime.BROWSER));
    assertTrue(TargetTags.includes(List.of("@skip-jvm"), TargetTags.Runtime.WORKER));
  }

  @Test
  public void unrelatedTagsDoNotChangeTheTarget() {
    assertIncludedInBoth(List.of("@smoke", "@integration"));
  }

  @Test
  public void combiningReservedTargetTagsIsAnError() {
    assertTrue(TargetTags.conflict(List.of("@browser", "@worker")).contains("conflicting"));
    assertTrue(TargetTags.conflict(List.of("@browser", "@portable")).contains("conflicting"));
    assertTrue(TargetTags.conflict(List.of("@worker", "@portable")).contains("conflicting"));
    assertTrue(TargetTags.conflict(List.of("@jvm", "@worker")).contains("conflicting"));
    assertTrue(TargetTags.conflict(List.of("@jvm", "@skip-jvm")).contains("conflicting"));
    assertNull(TargetTags.conflict(List.of("@browser", "@smoke")));
  }

  private void assertIncludedInBoth(List<String> tags) {
    assertTrue(TargetTags.includes(tags, TargetTags.Runtime.JVM));
    assertTrue(TargetTags.includes(tags, TargetTags.Runtime.BROWSER));
    assertTrue(TargetTags.includes(tags, TargetTags.Runtime.WORKER));
  }
}
