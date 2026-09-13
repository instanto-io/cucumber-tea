/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.gherkin.tea;

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Background;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleStepArgument;
import io.cucumber.messages.types.PickleStepType;
import io.cucumber.messages.types.Rule;
import io.cucumber.messages.types.RuleChild;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Step;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Uses the official parser to expose standard Gherkin as a compact executable model. */
public final class GherkinTeaParser {
  private final GherkinParser parser =
      GherkinParser.builder()
          .includeSource(false)
          .includeGherkinDocument(true)
          .includePickles(true)
          .build();

  /** Parses one feature and returns its concrete executable scenarios. */
  public GherkinFeature parse(String uri, byte[] source) {
    List<Envelope> envelopes = parser.parse(uri, source).toList();
    List<String> errors =
        envelopes.stream()
            .flatMap(envelope -> envelope.getParseError().stream())
            .map(error -> error.getMessage())
            .toList();
    if (!errors.isEmpty()) {
      throw new GherkinSyntaxException(uri + ": " + String.join("; ", errors));
    }

    GherkinDocument document =
        envelopes.stream()
            .flatMap(envelope -> envelope.getGherkinDocument().stream())
            .findFirst()
            .orElseThrow(() -> new GherkinSyntaxException(uri + ": no Gherkin document was produced"));
    Feature feature =
        document
            .getFeature()
            .orElseThrow(() -> new GherkinSyntaxException(uri + ": no Feature was found"));

    Map<String, Integer> stepLines = indexStepLines(feature);
    List<GherkinScenario> scenarios =
        envelopes.stream()
            .flatMap(envelope -> envelope.getPickle().stream())
            .map(pickle -> scenario(pickle, stepLines))
            .toList();
    return new GherkinFeature(uri, feature.getName(), scenarios);
  }

  private GherkinScenario scenario(Pickle pickle, Map<String, Integer> stepLines) {
    List<GherkinStep> steps = new ArrayList<>();
    for (PickleStep step : pickle.getSteps()) {
      int line =
          step.getAstNodeIds().stream()
              .map(stepLines::get)
              .filter(java.util.Objects::nonNull)
              .findFirst()
              .orElseGet(() -> pickle.getLocation().map(location -> location.getLine()).orElse(-1));
      steps.add(
          new GherkinStep(
              keyword(step.getType().orElse(PickleStepType.UNKNOWN)),
              step.getText(),
              line,
              argument(step.getArgument().orElse(null))));
    }

    return new GherkinScenario(
        pickle.getName(),
        pickle.getLocation().map(location -> location.getLine()).orElse(-1),
        pickle.getTags().stream().map(tag -> tag.getName()).toList(),
        steps);
  }

  private GherkinStepArgument argument(PickleStepArgument argument) {
    if (argument == null) {
      return null;
    }
    if (argument.getDocString().isPresent()) {
      var docString = argument.getDocString().orElseThrow();
      return new GherkinDocString(docString.getContent(), docString.getMediaType().orElse(""));
    }
    if (argument.getDataTable().isPresent()) {
      List<List<String>> rows =
          argument.getDataTable().orElseThrow().getRows().stream()
              .map(
                  row ->
                      row.getCells().stream()
                          .map(cell -> cell.getValue())
                          .toList())
              .toList();
      return new GherkinDataTable(rows);
    }
    return null;
  }

  private String keyword(PickleStepType type) {
    return switch (type) {
      case CONTEXT -> "Given";
      case ACTION -> "When";
      case OUTCOME -> "Then";
      case UNKNOWN -> "Step";
    };
  }

  private Map<String, Integer> indexStepLines(Feature feature) {
    Map<String, Integer> result = new HashMap<>();
    for (FeatureChild child : feature.getChildren()) {
      child.getBackground().ifPresent(background -> index(background, result));
      child.getScenario().ifPresent(scenario -> index(scenario, result));
      child.getRule().ifPresent(rule -> index(rule, result));
    }
    return result;
  }

  private void index(Background background, Map<String, Integer> result) {
    index(background.getSteps(), result);
  }

  private void index(Scenario scenario, Map<String, Integer> result) {
    index(scenario.getSteps(), result);
  }

  private void index(Rule rule, Map<String, Integer> result) {
    for (RuleChild child : rule.getChildren()) {
      child.getBackground().ifPresent(background -> index(background, result));
      child.getScenario().ifPresent(scenario -> index(scenario, result));
    }
  }

  private void index(List<Step> steps, Map<String, Integer> result) {
    for (Step step : steps) {
      result.put(step.getId(), step.getLocation().getLine());
    }
  }
}
