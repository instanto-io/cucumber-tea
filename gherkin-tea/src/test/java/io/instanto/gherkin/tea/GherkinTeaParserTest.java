/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class GherkinTeaParserTest {
  @Test
  public void expandsBackgroundsOutlinesTagsAndTablesWithTheOfficialParser() {
    String source =
        """
        @feature
        Feature: Arithmetic

          Background:
            Given a calculator

          @addition
          Scenario Outline: add a value
            Given the display shows <start>
            When I add <amount>
            Then the results are
              | expected |
              | <result> |

            Examples:
              | start | amount | result |
              | 40    | 2      | 42     |
              | 10    | 5      | 15     |
        """;

    GherkinFeature feature = new GherkinTeaParser().parse("features/arithmetic.feature", source.getBytes(UTF_8));

    assertEquals("Arithmetic", feature.name());
    assertEquals(2, feature.scenarios().size());
    GherkinScenario first = feature.scenarios().get(0);
    assertEquals("add a value", first.name());
    assertEquals(List.of("@feature", "@addition"), first.tags());
    assertEquals(4, first.steps().size());
    assertEquals("a calculator", first.steps().get(0).text());
    assertEquals("the display shows 40", first.steps().get(1).text());
    assertTrue(first.steps().get(3).argument() instanceof GherkinDataTable);
    GherkinDataTable table = (GherkinDataTable) first.steps().get(3).argument();
    assertEquals(List.of(List.of("expected"), List.of("42")), table.rows());
    assertEquals("the display shows 10", feature.scenarios().get(1).steps().get(1).text());
  }

  @Test(expected = GherkinSyntaxException.class)
  public void rejectsInvalidGherkinAtBuildTime() {
    new GherkinTeaParser().parse("features/broken.feature", "Scenario: orphan".getBytes(UTF_8));
  }

  @Test
  public void expandsRuleBackgroundsAndDocStrings() {
    String source =
        """
        Feature: Reporting

          Rule: Audited reports
            Background:
              Given auditing is enabled

            Scenario: write a report
              Then the report is
                \"""text/plain
                complete
                \"""
        """;

    GherkinFeature feature =
        new GherkinTeaParser().parse("features/reporting.feature", source.getBytes(UTF_8));

    GherkinScenario scenario = feature.scenarios().get(0);
    assertEquals(List.of("auditing is enabled", "the report is"),
        scenario.steps().stream().map(GherkinStep::text).toList());
    GherkinDocString docString = (GherkinDocString) scenario.steps().get(1).argument();
    assertEquals("complete", docString.content());
    assertEquals("text/plain", docString.mediaType());
  }
}
