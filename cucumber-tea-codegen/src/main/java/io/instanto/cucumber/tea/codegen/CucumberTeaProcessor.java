/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea.codegen;

import io.cucumber.cucumberexpressions.Argument;
import io.cucumber.cucumberexpressions.Expression;
import io.cucumber.cucumberexpressions.ExpressionFactory;
import io.cucumber.cucumberexpressions.ParameterTypeRegistry;
import io.instanto.cucumber.tea.AfterScenario;
import io.instanto.cucumber.tea.BeforeScenario;
import io.instanto.cucumber.tea.CucumberScript;
import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.DataTable;
import io.instanto.cucumber.tea.Given;
import io.instanto.cucumber.tea.Then;
import io.instanto.cucumber.tea.When;
import io.instanto.gherkin.tea.GherkinDataTable;
import io.instanto.gherkin.tea.GherkinDocString;
import io.instanto.gherkin.tea.GherkinFeature;
import io.instanto.gherkin.tea.GherkinTeaParser;
import io.instanto.gherkin.tea.GherkinScenario;
import io.instanto.gherkin.tea.GherkinStep;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/** Generates ordinary JUnit tests whose bodies contain only direct calls to step definitions. */
public final class CucumberTeaProcessor extends AbstractProcessor {
  static final String RUNNER_OPTION = "cucumber.tea.runner";

  private final Set<String> generatedSuites = new HashSet<>();
  private final GherkinTeaParser gherkin = new GherkinTeaParser();
  private final ExpressionFactory expressions =
      new ExpressionFactory(new ParameterTypeRegistry(Locale.ENGLISH));

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(CucumberSuite.class.getCanonicalName());
  }

  @Override
  public Set<String> getSupportedOptions() {
    return Set.of(RUNNER_OPTION);
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
    if (roundEnvironment.processingOver()) {
      return true;
    }
    for (Element element : roundEnvironment.getElementsAnnotatedWith(CucumberSuite.class)) {
      if (element instanceof TypeElement suite) {
        generate(suite);
      } else {
        error(element, "@CucumberSuite can only annotate a class");
      }
    }
    return true;
  }

  private void generate(TypeElement suite) {
    if (!generatedSuites.add(suite.getQualifiedName().toString())) {
      return;
    }
    if (!validateSuite(suite)) {
      return;
    }

    RunnerTarget target = runnerTarget(suite);
    if (target == null) {
      return;
    }
    Glue glue = glue(suite);
    if (glue == null) {
      return;
    }

    List<GherkinFeature> features = new ArrayList<>();
    CucumberSuite annotation = suite.getAnnotation(CucumberSuite.class);
    if (annotation.value().length == 0) {
      error(suite, "@CucumberSuite must name at least one feature resource");
      return;
    }
    for (String resource : annotation.value()) {
      GherkinFeature feature = readFeature(suite, resource);
      if (feature == null) {
        return;
      }
      features.add(feature);
    }

    List<BoundScenario> scenarios = bind(suite, glue, features);
    if (scenarios == null) {
      return;
    }
    List<BoundScenario> selected = select(suite, target, scenarios);
    if (selected != null && !selected.isEmpty()) {
      writeSuite(suite, target, glue, selected);
    }
  }

  private boolean validateSuite(TypeElement suite) {
    boolean valid = true;
    if (suite.getKind() != ElementKind.CLASS || suite.getNestingKind() != NestingKind.TOP_LEVEL) {
      error(suite, "@CucumberSuite currently requires a top-level class");
      valid = false;
    }
    if (suite.getModifiers().contains(Modifier.ABSTRACT)
        || suite.getModifiers().contains(Modifier.PRIVATE)) {
      error(suite, "A Cucumber Tea step class must be concrete and constructible");
      valid = false;
    }

    List<ExecutableElement> constructors =
        suite.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
            .map(ExecutableElement.class::cast)
            .toList();
    if (!constructors.isEmpty()
        && constructors.stream()
            .noneMatch(
                constructor ->
                    constructor.getParameters().isEmpty()
                        && !constructor.getModifiers().contains(Modifier.PRIVATE))) {
      error(suite, "A Cucumber Tea step class needs an accessible no-argument constructor");
      valid = false;
    }
    return valid;
  }

  private RunnerTarget runnerTarget(Element element) {
    String configured = processingEnv.getOptions().getOrDefault(RUNNER_OPTION, "teavm");
    return switch (configured.toLowerCase(Locale.ROOT)) {
      case "jvm" -> RunnerTarget.JVM;
      case "teavm" -> RunnerTarget.TEAVM;
      case "miniflare" -> RunnerTarget.MINIFLARE;
      default -> {
        error(element, "Unknown -A" + RUNNER_OPTION + " value '" + configured
            + "'; expected jvm, teavm or miniflare");
        yield null;
      }
    };
  }

  private Glue glue(TypeElement suite) {
    List<StepDefinition> definitions = new ArrayList<>();
    List<ExecutableElement> before = new ArrayList<>();
    List<ExecutableElement> after = new ArrayList<>();
    boolean valid = true;

    for (Element member : processingEnv.getElementUtils().getAllMembers(suite)) {
      if (!(member instanceof ExecutableElement method) || member.getKind() != ElementKind.METHOD) {
        continue;
      }
      String source = stepExpression(method);
      if (source != null) {
        if (!validateCallable(suite, method, false)) {
          valid = false;
        } else {
          try {
            definitions.add(new StepDefinition(method, source, expressions.createExpression(source)));
          } catch (RuntimeException failure) {
            error(method, "Invalid Cucumber Expression '" + source + "': " + failure.getMessage());
            valid = false;
          }
        }
      }
      if (method.getAnnotation(BeforeScenario.class) != null) {
        if (validateCallable(suite, method, true)) {
          before.add(method);
        } else {
          valid = false;
        }
      }
      if (method.getAnnotation(AfterScenario.class) != null) {
        if (validateCallable(suite, method, true)) {
          after.add(method);
        } else {
          valid = false;
        }
      }
    }
    if (definitions.isEmpty()) {
      error(suite, "A Cucumber Tea suite needs at least one @Given, @When, or @Then method");
      valid = false;
    }
    if (!valid) {
      return null;
    }
    Collections.reverse(after);
    return new Glue(definitions, before, after);
  }

  private String stepExpression(ExecutableElement method) {
    Given given = method.getAnnotation(Given.class);
    When when = method.getAnnotation(When.class);
    Then then = method.getAnnotation(Then.class);
    int count = (given == null ? 0 : 1) + (when == null ? 0 : 1) + (then == null ? 0 : 1);
    if (count > 1) {
      error(method, "A step method must have only one of @Given, @When, or @Then");
      return null;
    }
    if (given != null) {
      return given.value();
    }
    if (when != null) {
      return when.value();
    }
    return then == null ? null : then.value();
  }

  private boolean validateCallable(
      TypeElement suite, ExecutableElement method, boolean hook) {
    boolean valid = true;
    if (method.getModifiers().contains(Modifier.PRIVATE)) {
      error(method, "Cucumber Tea methods must not be private");
      valid = false;
    }
    String suitePackage =
        processingEnv.getElementUtils().getPackageOf(suite).getQualifiedName().toString();
    String methodPackage =
        processingEnv.getElementUtils().getPackageOf(method).getQualifiedName().toString();
    if (!method.getModifiers().contains(Modifier.PUBLIC)
        && !suitePackage.equals(methodPackage)) {
      error(method, "Inherited Cucumber Tea methods declared in another package must be public");
      valid = false;
    }
    if (method.getReturnType().getKind() != TypeKind.VOID) {
      error(method, "Cucumber Tea methods must return void");
      valid = false;
    }
    if (hook && !method.getParameters().isEmpty()) {
      error(method, "Cucumber Tea scenario hooks must not take parameters");
      valid = false;
    }
    return valid;
  }

  private GherkinFeature readFeature(Element suite, String resource) {
    String normalized = resource.startsWith("/") ? resource.substring(1) : resource;
    Exception outputFailure;
    try {
      FileObject file =
          processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", normalized);
      try (InputStream input = file.openInputStream()) {
        return gherkin.parse(normalized, input.readAllBytes());
      }
    } catch (Exception failure) {
      outputFailure = failure;
    }
    try {
      FileObject file =
          processingEnv.getFiler().getResource(StandardLocation.CLASS_PATH, "", normalized);
      try (InputStream input = file.openInputStream()) {
        return gherkin.parse(normalized, input.readAllBytes());
      }
    } catch (Exception classPathFailure) {
      error(
          suite,
          "Could not read Gherkin feature '"
              + resource
              + "' from the test output or classpath: "
              + outputFailure.getMessage()
              + "; "
              + classPathFailure.getMessage());
      return null;
    }
  }

  private List<BoundScenario> bind(
      Element suite, Glue glue, List<GherkinFeature> features) {
    List<BoundScenario> result = new ArrayList<>();
    boolean valid = true;
    int ordinal = 0;
    for (GherkinFeature feature : features) {
      for (GherkinScenario scenario : feature.scenarios()) {
        List<BoundStep> steps = new ArrayList<>();
        for (GherkinStep step : scenario.steps()) {
          List<BoundStep> matches = new ArrayList<>();
          for (StepDefinition definition : glue.definitions()) {
            BoundStep match = match(feature, scenario, step, definition);
            if (match != null) {
              matches.add(match);
            }
          }
          if (matches.isEmpty()) {
            error(
                suite,
                feature.uri() + ":" + step.line() + ": undefined step: " + step.text());
            valid = false;
          } else if (matches.size() > 1) {
            error(
                suite,
                feature.uri() + ":" + step.line() + ": ambiguous step: " + step.text()
                    + " matches "
                    + matches.stream()
                        .map(match -> match.definition().expressionSource())
                        .toList());
            valid = false;
          } else {
            steps.add(matches.get(0));
          }
        }
        result.add(new BoundScenario(feature, scenario, steps, ordinal++));
      }
    }
    return valid ? result : null;
  }

  private List<BoundScenario> select(
      Element suite, RunnerTarget target, List<BoundScenario> scenarios) {
    boolean valid = true;
    List<BoundScenario> selected = new ArrayList<>();
    for (BoundScenario bound : scenarios) {
      String conflict = TargetTags.conflict(bound.scenario().tags());
      if (conflict != null) {
        error(
            suite,
            bound.feature().uri() + ":" + bound.scenario().line() + ": " + conflict);
        valid = false;
      } else if (TargetTags.includes(bound.scenario().tags(), target.runtime)) {
        selected.add(bound);
      }
    }
    return valid ? selected : null;
  }

  private BoundStep match(
      GherkinFeature feature,
      GherkinScenario scenario,
      GherkinStep step,
      StepDefinition definition) {
    List<? extends VariableElement> parameters = definition.method().getParameters();
    int expressionParameterCount = parameters.size();
    if (step.argument() != null) {
      if (parameters.isEmpty() || !argumentTypeMatches(parameters.get(parameters.size() - 1), step)) {
        return null;
      }
      expressionParameterCount--;
    }

    Type[] parameterTypes = new Type[expressionParameterCount];
    for (int index = 0; index < expressionParameterCount; index++) {
      Class<?> parameterType = parameterClass(parameters.get(index).asType());
      if (parameterType == null) {
        return null;
      }
      parameterTypes[index] = parameterType;
    }

    Optional<List<Argument<?>>> matched;
    try {
      matched = definition.expression().match(step.text(), parameterTypes);
    } catch (RuntimeException ignored) {
      return null;
    }
    if (matched.isEmpty()) {
      return null;
    }

    List<String> arguments = new ArrayList<>();
    for (Argument<?> argument : matched.orElseThrow()) {
      arguments.add(literal(argument.getValue()));
    }
    if (step.argument() instanceof GherkinDocString docString) {
      arguments.add(quote(docString.content()));
    } else if (step.argument() instanceof GherkinDataTable dataTable) {
      arguments.add(tableLiteral(dataTable));
    }
    return new BoundStep(feature, scenario, step, definition, arguments);
  }

  private boolean argumentTypeMatches(VariableElement parameter, GherkinStep step) {
    String type = processingEnv.getTypeUtils().erasure(parameter.asType()).toString();
    if (step.argument() instanceof GherkinDocString) {
      return type.equals(String.class.getCanonicalName());
    }
    if (step.argument() instanceof GherkinDataTable) {
      return type.equals(DataTable.class.getCanonicalName());
    }
    return false;
  }

  private Class<?> parameterClass(TypeMirror mirror) {
    return switch (mirror.getKind()) {
      case BYTE -> byte.class;
      case SHORT -> short.class;
      case INT -> int.class;
      case LONG -> long.class;
      case FLOAT -> float.class;
      case DOUBLE -> double.class;
      case BOOLEAN -> boolean.class;
      case CHAR -> char.class;
      case DECLARED -> declaredClass(processingEnv.getTypeUtils().erasure(mirror).toString());
      default -> null;
    };
  }

  private Class<?> declaredClass(String name) {
    if (name.equals(String.class.getCanonicalName())) return String.class;
    if (name.equals(Byte.class.getCanonicalName())) return Byte.class;
    if (name.equals(Short.class.getCanonicalName())) return Short.class;
    if (name.equals(Integer.class.getCanonicalName())) return Integer.class;
    if (name.equals(Long.class.getCanonicalName())) return Long.class;
    if (name.equals(Float.class.getCanonicalName())) return Float.class;
    if (name.equals(Double.class.getCanonicalName())) return Double.class;
    if (name.equals(Boolean.class.getCanonicalName())) return Boolean.class;
    if (name.equals(Character.class.getCanonicalName())) return Character.class;
    if (name.equals(BigInteger.class.getCanonicalName())) return BigInteger.class;
    if (name.equals(BigDecimal.class.getCanonicalName())) return BigDecimal.class;
    return null;
  }

  private String literal(Object value) {
    if (value instanceof String string) return quote(string);
    if (value instanceof Byte number) return "(byte) " + number;
    if (value instanceof Short number) return "(short) " + number;
    if (value instanceof Integer number) return number.toString();
    if (value instanceof Long number) return number + "L";
    if (value instanceof Float number) return number + "F";
    if (value instanceof Double number) return number + "D";
    if (value instanceof Boolean bool) return bool.toString();
    if (value instanceof Character character) return "'" + escape(character) + "'";
    if (value instanceof BigInteger number) {
      return "new java.math.BigInteger(" + quote(number.toString()) + ")";
    }
    if (value instanceof BigDecimal number) {
      return "new java.math.BigDecimal(" + quote(number.toString()) + ")";
    }
    throw new IllegalArgumentException("Unsupported generated argument type: " + value.getClass());
  }

  private String tableLiteral(GherkinDataTable table) {
    StringBuilder result = new StringBuilder("io.instanto.cucumber.tea.DataTable.of(new String[][] {");
    for (List<String> row : table.rows()) {
      result.append("{");
      for (String cell : row) {
        result.append(quote(cell)).append(",");
      }
      result.append("},");
    }
    return result.append("})").toString();
  }

  private void writeSuite(
      TypeElement suite, RunnerTarget target, Glue glue, List<BoundScenario> scenarios) {
    String packageName = processingEnv.getElementUtils().getPackageOf(suite).getQualifiedName().toString();
    String generatedSimpleName = suite.getSimpleName() + "Test";
    String generatedName =
        packageName.isEmpty() ? generatedSimpleName : packageName + "." + generatedSimpleName;
    try {
      JavaFileObject source = processingEnv.getFiler().createSourceFile(generatedName, suite);
      try (Writer writer = source.openWriter()) {
        String contexts = contextProvider(suite);
        String runnerClass = runnerClass(suite, target);
        if (target != RunnerTarget.JVM && contexts != null) {
          error(suite, "@CucumberSuite contexts are currently supported by the JVM target only");
          return;
        }
        if (contexts != null && !java.util.Objects.equals(runnerClass, target.runnerClass)) {
          error(suite, "@CucumberSuite cannot combine contexts with a custom runner");
          return;
        }
        if (!packageName.isEmpty()) {
          writer.write("package " + packageName + ";\n\n");
        }
        writer.write("@" + Generated.class.getCanonicalName() + "(\""
            + CucumberTeaProcessor.class.getCanonicalName() + "\")\n");
        if (runnerClass != null) {
          writer.write("@org.junit.runner.RunWith(" + runnerClass + ".class)\n");
        } else if (contexts != null) {
          writer.write("@org.junit.runner.RunWith(org.junit.runners.Parameterized.class)\n");
        }
        writer.write("public final class " + generatedSimpleName + " {\n");
        if (contexts != null) {
          writer.write("  @org.junit.runners.Parameterized.Parameters(name = \"{0}\")\n");
          writer.write("  public static java.util.List<io.instanto.cucumber.tea.CucumberContext>"
              + " cucumberTeaContexts() {\n");
          writer.write("    return new " + contexts + "().contexts();\n");
          writer.write("  }\n\n");
          writer.write("  @org.junit.runners.Parameterized.Parameter\n");
          writer.write("  public io.instanto.cucumber.tea.CucumberContext cucumberTeaContext;\n\n");
        }
        for (BoundScenario scenario : scenarios) {
          writeScenario(
              writer,
              suite,
              target,
              suite.getAnnotation(CucumberSuite.class),
              glue,
              scenario,
              contexts != null);
        }
        writer.write("}\n");
      }
    } catch (IOException failure) {
      error(suite, "Could not generate " + generatedName + ": " + failure.getMessage());
    }
  }

  private String contextProvider(TypeElement suite) {
    String annotationName = CucumberSuite.class.getCanonicalName();
    String none = "io.instanto.cucumber.tea.CucumberContexts.None";
    for (var annotation : suite.getAnnotationMirrors()) {
      if (!annotation.getAnnotationType().toString().equals(annotationName)) {
        continue;
      }
      for (var entry :
          processingEnv.getElementUtils().getElementValuesWithDefaults(annotation).entrySet()) {
        if (!entry.getKey().getSimpleName().contentEquals("contexts")) {
          continue;
        }
        String provider = entry.getValue().getValue().toString();
        return provider.equals(none) ? null : provider;
      }
    }
    return null;
  }

  private String runnerClass(TypeElement suite, RunnerTarget target) {
    String annotationName = CucumberSuite.class.getCanonicalName();
    String defaultRunner = CucumberSuite.DefaultRunner.class.getCanonicalName();
    for (var annotation : suite.getAnnotationMirrors()) {
      if (!annotation.getAnnotationType().toString().equals(annotationName)) {
        continue;
      }
      for (var entry :
          processingEnv.getElementUtils().getElementValuesWithDefaults(annotation).entrySet()) {
        if (!entry.getKey().getSimpleName().contentEquals("runner")) {
          continue;
        }
        String configured = entry.getValue().getValue().toString();
        return configured.equals(defaultRunner) ? target.runnerClass : configured;
      }
    }
    return target.runnerClass;
  }

  private void writeScripts(Writer writer, CucumberSuite suite) throws IOException {
    for (CucumberScript script : suite.scripts()) {
      writer.write("  @org.teavm.junit.ServeJS(from = " + quote(script.resource())
          + ", as = " + quote(script.path()) + ")\n");
    }
    if (suite.scripts().length > 0) {
      writer.write("  @org.teavm.junit.AttachJavaScript({");
      for (CucumberScript script : suite.scripts()) {
        writer.write(quote(script.resource()) + ",");
      }
      writer.write("})\n");
    }
  }

  private void writeScenario(
      Writer writer,
      TypeElement suite,
      RunnerTarget target,
      CucumberSuite annotation,
      Glue glue,
      BoundScenario bound,
      boolean contextual)
      throws IOException {
    GherkinFeature feature = bound.feature();
    GherkinScenario scenario = bound.scenario();
    if (target == RunnerTarget.TEAVM) {
      writeScripts(writer, annotation);
    }
    if (target != RunnerTarget.JVM && scenario.tags().contains(TargetTags.SKIP_JVM)) {
      writer.write("  @org.teavm.junit.SkipJVM\n");
    }
    writer.write("  @org.junit.Test\n");
    writer.write("  public void " + methodName(scenario, bound.ordinal()) + "() throws Throwable {\n");
    if (contextual) {
      writer.write(
          "    io.instanto.cucumber.tea.CucumberTea.withContext(cucumberTeaContext, () -> {\n");
    }
    writer.write("    final " + suite.getQualifiedName() + " glue = new "
        + suite.getQualifiedName() + "();\n");
    writer.write("    io.instanto.cucumber.tea.CucumberTea.scenario("
        + quote(feature.name()) + ", " + quote(scenario.name()) + ",\n");
    writer.write("        () -> {\n");
    for (ExecutableElement hook : glue.before()) {
      writeHook(writer, feature, scenario, "Before", hook);
    }
    writer.write("        },\n        () -> {\n");
    for (BoundStep step : bound.steps()) {
      writeStep(writer, step);
    }
    writer.write("        },\n        () -> {\n");
    if (!glue.after().isEmpty()) {
      writer.write("          io.instanto.cucumber.tea.CucumberTea.afterHooks(\n");
      for (int index = 0; index < glue.after().size(); index++) {
        ExecutableElement hook = glue.after().get(index);
        writer.write("              () -> " + hookCall(feature, scenario, "After", hook));
        writer.write(index + 1 == glue.after().size() ? ");\n" : ",\n");
      }
    }
    writer.write("        });\n");
    if (contextual) {
      writer.write("    });\n");
    }
    writer.write("  }\n\n");
  }

  private void writeHook(
      Writer writer,
      GherkinFeature feature,
      GherkinScenario scenario,
      String kind,
      ExecutableElement hook)
      throws IOException {
    writer.write("          " + hookCall(feature, scenario, kind, hook) + ";\n");
  }

  private String hookCall(
      GherkinFeature feature,
      GherkinScenario scenario,
      String kind,
      ExecutableElement hook) {
    return "io.instanto.cucumber.tea.CucumberTea.hook("
        + quote(feature.name()) + ", " + quote(scenario.name()) + ", " + quote(kind)
        + ", () -> glue." + hook.getSimpleName() + "())";
  }

  private void writeStep(Writer writer, BoundStep bound) throws IOException {
    GherkinFeature feature = bound.feature();
    GherkinScenario scenario = bound.scenario();
    GherkinStep step = bound.step();
    String call = "glue." + bound.definition().method().getSimpleName() + "("
        + String.join(", ", bound.arguments()) + ")";
    writer.write("          io.instanto.cucumber.tea.CucumberTea.step("
        + quote(feature.name()) + ", " + quote(scenario.name()) + ", "
        + quote(feature.uri() + ":" + step.line()) + ", " + quote(step.keyword()) + ", "
        + quote(step.text()) + ", () -> " + call + ");\n");
  }

  private String methodName(GherkinScenario scenario, int ordinal) {
    String normalized = scenario.name().replaceAll("[^A-Za-z0-9]+", "_");
    if (normalized.length() > 70) {
      normalized = normalized.substring(0, 70);
    }
    if (normalized.isEmpty()) {
      normalized = "unnamed";
    }
    return "scenario_" + normalized + "_line_" + scenario.line() + "_" + ordinal;
  }

  private String quote(String value) {
    StringBuilder result = new StringBuilder("\"");
    for (int index = 0; index < value.length(); index++) {
      result.append(escape(value.charAt(index)));
    }
    return result.append('"').toString();
  }

  private String escape(char character) {
    return switch (character) {
      case '\\' -> "\\\\";
      case '"' -> "\\\"";
      case '\n' -> "\\n";
      case '\r' -> "\\r";
      case '\t' -> "\\t";
      case '\b' -> "\\b";
      case '\f' -> "\\f";
      default -> character < 32 ? String.format("\\u%04x", (int) character) : String.valueOf(character);
    };
  }

  private void error(Element element, String message) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
  }

  private enum RunnerTarget {
    JVM(null, TargetTags.Runtime.JVM),
    TEAVM("org.teavm.junit.TeaVMTestRunner", TargetTags.Runtime.BROWSER),
    MINIFLARE(
        "io.instanto.sarto.edge.cf.junit.MiniflareTestRunner", TargetTags.Runtime.WORKER);

    private final String runnerClass;
    private final TargetTags.Runtime runtime;

    RunnerTarget(String runnerClass, TargetTags.Runtime runtime) {
      this.runnerClass = runnerClass;
      this.runtime = runtime;
    }
  }

  private record StepDefinition(
      ExecutableElement method, String expressionSource, Expression expression) {}

  private record Glue(
      List<StepDefinition> definitions,
      List<ExecutableElement> before,
      List<ExecutableElement> after) {}

  private record BoundStep(
      GherkinFeature feature,
      GherkinScenario scenario,
      GherkinStep step,
      StepDefinition definition,
      List<String> arguments) {}

  private record BoundScenario(
      GherkinFeature feature, GherkinScenario scenario, List<BoundStep> steps, int ordinal) {}
}
