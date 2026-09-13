# Gherkin Tea and Cucumber Tea

Gherkin describes application behaviour in readable `.feature` files.
Cucumber connects each sentence in those files to test code and orchestrates
the scenarios. Gherkin Tea and Cucumber Tea bring that same arrangement to
Java integration tests compiled by [TeaVM](https://teavm.org/).

| Familiar part | TeaVM companion |
| --- | --- |
| Gherkin feature files and parser | **Gherkin Tea** uses the official Gherkin parser during the build and exposes the concrete scenarios needed by a test generator. |
| Cucumber steps and orchestration | **Cucumber Tea** binds those scenarios to annotated Java methods and generates JUnit tests for a TeaVM host. |

The feature files remain standard Gherkin. The test code remains Java. The
generated tests can run on the JVM, in Chromium with `TeaVMTestRunner`, or
inside a Cloudflare Worker development environment with Sarto Edge's
`MiniflareTestRunner`.

## Status

This is a pre-release project and its Maven artifacts are not yet published.
It requires JDK 21. To try the current `0.1.0-SNAPSHOT` locally, run
`./mvnw install` before adding the dependencies below to another build.

## Contents

| Area | Jump to |
| --- | --- |
| Start here | [First feature](#write-a-feature) · [Step class](#connect-it-to-java) · [Maven setup](#add-cucumber-tea-to-a-test-module) · [Run it](#run-in-chromium) |
| Apply it | [JVM contexts](#run-one-specification-against-explicit-jvm-contexts) · [React components](#test-a-react-component) · [Cloudflare Workers](#run-in-miniflare) · [Organise a test library](#organise-a-larger-test-library) |
| Use more Gherkin | [Values](#pass-values-to-java) · [Tables and text](#pass-tables-and-text-blocks) · [Outlines](#repeat-a-scenario-with-examples) · [Lifecycle](#prepare-and-clean-up-a-scenario) |
| Reference | [Runner tags](#choose-an-execution-host) · [Build boundary](#what-runs-where) · [Modules](#modules) · [Current scope](#current-scope) |

## Write a feature

Put feature files in `src/test/resources`. This example describes calculator
behaviour without choosing a UI toolkit or test runner:

```gherkin
Feature: Arithmetic

  Scenario: add a value
    Given the display shows 40
    When I add 2
    Then the result is 42
```

Because this is ordinary Gherkin, another implementation of the application
can use the same feature. A React implementation and a Verrai implementation,
for example, can each provide Java step methods appropriate to their UI.

## Connect it to Java

Name the feature resource on a step class and connect its sentences to Java
methods:

```java
import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Given;
import io.instanto.cucumber.tea.Then;
import io.instanto.cucumber.tea.When;

import static org.junit.Assert.assertEquals;

@CucumberSuite("features/arithmetic.feature")
public final class ArithmeticSteps {
    private int display;

    @Given("the display shows {int}")
    public void displayShows(int value) {
        display = value;
    }

    @When("I add {int}")
    public void add(int value) {
        display += value;
    }

    @Then("the result is {int}")
    public void resultIs(int expected) {
        assertEquals(expected, display);
    }
}
```

`{int}` is a Cucumber Expression parameter. It matches the number in the
sentence and supplies it as the Java argument. Cucumber Tea uses the official
[Cucumber Expressions](https://github.com/cucumber/cucumber-expressions)
implementation when it checks and binds the methods.

During `testCompile`, Cucumber Tea generates an ordinary JUnit test class named
after the step class: `ArithmeticSteps` becomes `ArithmeticStepsTest`. There is
no runtime classpath scanning: undefined or ambiguous steps fail the Java build
before TeaVM starts.

The complete executable version is in
[`cucumber-tea-examples`](cucumber-tea-examples/src/test/java/io/instanto/cucumber/tea/examples/ArithmeticSteps.java).

## Add Cucumber Tea to a test module

Add the small runtime used by the generated tests:

```xml
<properties>
  <cucumber-tea.version>0.1.0-SNAPSHOT</cucumber-tea.version>
  <teavm.version>0.15.0</teavm.version>
</properties>

<dependencies>
  <dependency>
    <groupId>io.instanto</groupId>
    <artifactId>cucumber-tea</artifactId>
    <version>${cucumber-tea.version}</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.teavm</groupId>
    <artifactId>teavm-classlib</artifactId>
    <version>${teavm.version}</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.teavm</groupId>
    <artifactId>teavm-junit</artifactId>
    <version>${teavm.version}</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Put the generator on Maven's annotation-processor path:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.15.0</version>
      <executions>
        <execution>
          <id>default-testCompile</id>
          <configuration>
            <proc>full</proc>
            <annotationProcessorPaths>
              <path>
                <groupId>io.instanto</groupId>
                <artifactId>cucumber-tea-codegen</artifactId>
                <version>${cucumber-tea.version}</version>
              </path>
            </annotationProcessorPaths>
            <annotationProcessors>
              <annotationProcessor>io.instanto.cucumber.tea.codegen.CucumberTeaProcessor</annotationProcessor>
            </annotationProcessors>
            <compilerArgs>
              <arg>-Acucumber.tea.runner=teavm</arg>
            </compilerArgs>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Feature files can belong in `src/test/resources` or in a test dependency. Maven
copies local resources to the test output before the processor reads them; a
shared feature artifact is read from the test classpath. The processor brings
Gherkin Tea and the official parser onto its isolated path; neither is a test
runtime dependency.

The example modules in this repository list `cucumber-tea-codegen` as a test
dependency because Maven builds the generator and its examples together in one
reactor. An application consumes an already-built release and should keep the
generator on the isolated annotation-processor path shown above.

## Run one specification against explicit JVM contexts

Set the processor target to `jvm` when the step code coordinates JVM-hosted
test infrastructure:

```xml
<compilerArgs>
  <arg>-Acucumber.tea.runner=jvm</arg>
</compilerArgs>
```

A suite may then name one context provider. The generated test executes every
scenario once for each named value:

```java
@CucumberSuite(
    value = "features/service-round-trip.feature",
    contexts = ServiceTopologies.class)
public final class ServiceRoundTripSteps {
    @When("the service is called")
    public void callService() {
        var topology = CucumberTea.context(ServiceTopology.class);
        topology.callService();
    }
}
```

The annotation remains the single association between the readable feature,
its glue, and the topology matrix. Context names appear in the JUnit results.
The complete executable example is in
[`cucumber-tea-examples-jvm`](cucumber-tea-examples-jvm).

## Start host infrastructure with a suite runner

Some specifications need a JUnit runner that starts their host before executing
the generated scenarios. Declare that runner on the suite rather than building
the infrastructure in the step class:

```java
@CucumberSuite(
    value = "features/service-round-trip.feature",
    runner = TopologyTeaVmTestRunner.class)
public final class ServiceRoundTripSteps {
    // Gherkin step methods
}
```

The processor target still decides which scenarios belong in the generated
suite and whether browser or Worker support is emitted. `runner` only replaces
the target's standard JUnit runner, allowing a topology, container or host-aware
runner to prepare the environment around the same generated test methods.

A suite cannot declare both `contexts` and a custom `runner`: explicit contexts
already require JUnit's parameterized runner. Use a host runner when the
environment surrounds the whole suite, and contexts when the same JVM scenarios
should execute once for each declared topology.

The custom-runner example in
[`cucumber-tea-examples-jvm`](cucumber-tea-examples-jvm) proves that the selected
runner is active while its generated Gherkin scenario executes.

## Run in Chromium

Run the Maven test normally:

```shell
./mvnw clean test
```

The generated class uses `TeaVMTestRunner`. TeaVM compiles the test, the step
class, and the application code they reach to JavaScript, then executes the
scenario in Chromium.

## Test a React component

TeaVM React's `teavm-react-testkit` mounts a component for one scenario and
flushes React updates before a step examines the DOM. [Mockatcha
DOM](https://github.com/instanto-io/webapp-testkit/tree/main/webapp-testkit-dom) can provide
the DOM queries, events, and assertions.

A feature can stay focused on visible behaviour:

```gherkin
@browser @skip-jvm
Scenario: add a timesheet row
  Given the weekly timesheet is open
  When I add an entry for "Design" lasting 2 hours
  Then the timesheet contains
    | activity | hours |
    | Design   | 2     |
```

The step class supplies React's test scripts and owns one mounted root:

```java
import static ca.weblite.teavmreact.testing.ReactTestScripts.*;

@CucumberSuite(
    value = "features/timesheet.feature",
    scripts = {
      @CucumberScript(resource = REACT_RESOURCE, path = REACT_PATH),
      @CucumberScript(resource = REACT_DOM_RESOURCE, path = REACT_DOM_PATH)
    })
public final class TimesheetSteps {
    private ReactTestRoot root;

    @BeforeScenario
    public void mountTimesheet() {
        root = ReactTestRoot.mount(testContainer(), timesheet.element());
    }

    @When("I add an entry for {string} lasting {int} hours")
    public void addEntry(String activity, int hours) {
        ReactTest.type(find("[name=activity]"), activity);
        ReactTest.type(find("[name=hours]"), Integer.toString(hours));
        ReactTest.flush(() -> click(find("button[type=submit]")));
    }

    @AfterScenario
    public void unmountTimesheet() {
        root.close();
    }
}
```

The React-specific mounting support belongs to TeaVM React; Cucumber Tea only
orchestrates the feature and steps.

## Run in Miniflare

Cucumber Tea can instead generate the JUnit class expected by Sarto Edge's
`MiniflareTestRunner`:

```xml
<compilerArgs>
  <arg>-Acucumber.tea.runner=miniflare</arg>
</compilerArgs>
```

Add `io.instanto:sarto-edge-cf-junit` as a test dependency. The runner compiles
the generated JUnit test with TeaVM and executes its JavaScript inside the
Miniflare development environment. Worker APIs and bindings are therefore
available to the step code in the same kind of host as the application.

This is an optional integration. Gherkin Tea and Cucumber Tea do not otherwise
depend on Sarto Edge or its companion libraries.

## Pass values to Java

The built-in Cucumber Expression parameters include `{string}`, `{int}`, and
the other common numeric types:

```gherkin
When "Ada" places an order for 2 items
```

```java
@When("{string} places an order for {int} items")
public void placeOrder(String customer, int quantity) {
    orders.place(customer, quantity);
}
```

## Pass tables and text blocks

A table is delivered as an immutable `DataTable` after the expression
arguments:

```java
@Then("the timesheet contains")
public void containsRows(DataTable expected) {
    assertEquals("Design", expected.cell(1, 0));
    assertEquals("2", expected.cell(1, 1));
}
```

A Gherkin doc string is supplied as the final `String` argument. Values in
tables and doc strings are filled in when they appear in a Scenario Outline.

## Repeat a scenario with examples

A Scenario Outline avoids copying the same behaviour for several values:

```gherkin
Scenario Outline: total a week
  Given the timesheet contains <hours> hours
  Then its total is <hours> hours

  Examples:
    | hours |
    | 0     |
    | 7     |
```

The official parser turns those rows into two concrete scenarios during the
build. Cucumber Tea generates a separate JUnit method for each one.

## Prepare and clean up a scenario

Use hooks for technical fixture work that would distract from the feature:

```java
@BeforeScenario
public void openFixture() {
    fixture = new TimesheetFixture();
}

@AfterScenario
public void closeFixture() {
    fixture.close();
}
```

Cucumber Tea creates a fresh step-class object for every scenario. Before
hooks run before Background and scenario steps. Every after hook is attempted,
even when a step or an earlier after hook fails. Later failures are suppressed
on the first failure.

Use a Gherkin `Background` instead when the starting condition helps a reader
understand every scenario in the feature.

## Choose an execution host

Cucumber Tea reserves five tags:

| Tag | Meaning |
| --- | --- |
| no host tag | Include the scenario in every generated target. |
| `@portable` | Explicitly include the scenario in every target. |
| `@jvm` | Include it only when generating ordinary JVM tests. |
| `@browser` | Include it only when generating TeaVM Chromium tests. |
| `@worker` | Include it only when generating Miniflare tests. |
| `@skip-jvm` | Exclude it from the JVM target and add TeaVM's `@SkipJVM` to JavaScript-hosted methods. |

Use `@skip-jvm` when a scenario calls a DOM, Worker, or other JavaScript-only
API. It controls TeaVM's preliminary JVM execution; it does not select a host,
so pair it with `@browser` or `@worker` when the scenario is host-specific.

## Organise a larger test library

Group features by application capability rather than by runner. Keep the
feature readable, put its vocabulary in a focused step class, keep mutable
state local to one scenario, and hide DOM or Worker mechanics behind a test
kit. The executable
[`cucumber-tea-examples-organisation`](cucumber-tea-examples-organisation/README.md)
module develops that structure with several features and two hosts.

## What runs where

The build boundary keeps the target test small:

1. Gherkin Tea calls the official parser during `testCompile`.
2. Cucumber Tea checks each feature sentence against the annotated Java
   methods using Cucumber Expressions.
3. It generates direct Java calls inside ordinary JUnit methods.
4. TeaVM compiles those generated methods, their small runtime support, the
   step class, and the application code they use.
5. `TeaVMTestRunner` starts Chromium, or `MiniflareTestRunner` starts the
   Worker host.

The parser, Cucumber message model, expression matcher, and annotation
processor stay on the build side and are not included in the generated
JavaScript.

Gherkin Tea deliberately wraps the official parser rather than creating a new
Gherkin dialect. The official parser's test suite defines Gherkin parsing; the
Gherkin Tea tests check how executable scenarios are mapped into its smaller
model. The generated browser and Worker examples verify the boundary that is
specific to TeaVM.

## Modules

| Module | Purpose |
| --- | --- |
| `gherkin-tea` | Build-time adapter from the official parser to a compact executable-scenario model. |
| `cucumber-tea` | Annotations, `DataTable`, and small scenario failure support used by generated tests. |
| `cucumber-tea-codegen` | Step binding, target selection, validation, and JUnit source generation. |
| `cucumber-tea-examples` | Compact executable examples in Chromium and Miniflare. |
| `cucumber-tea-examples-jvm` | An executable JVM suite parameterized by explicit named contexts. |
| `cucumber-tea-examples-organisation` | A multi-feature example showing test-library organisation. |

Most applications declare `cucumber-tea` as a test dependency and put
`cucumber-tea-codegen` on the annotation-processor path.

## Current scope

Cucumber Tea currently supports:

- standard Gherkin parsing through the official parser;
- Backgrounds, Rules, localisation, tags, and Scenario Outlines;
- standard Cucumber Expressions for scalar Java arguments;
- doc strings and immutable data tables;
- inherited step methods and before/after scenario hooks;
- compile-time checks for undefined, ambiguous, or conflicting steps and
  target tags;
- generated JUnit 4 tests for ordinary JVM and explicit context-matrix execution;
- generated JUnit 4 tests for TeaVM's Chromium runner; and
- generated JUnit 4 tests for Sarto Edge's Miniflare runner.

It is not a port of Cucumber JVM's runner or plug-in system. Its job is to
reuse standard Gherkin and Cucumber step conventions while allowing TeaVM's
existing runners to own JavaScript compilation and target execution.

## Build this repository

Run all browser examples with:

```shell
./mvnw clean test
```

When the Sarto Edge runner and development container are available locally,
run the Worker examples with:

```shell
./mvnw clean test -Pminiflare
```

## License

Gherkin Tea and Cucumber Tea are available under the
[Apache License, Version 2.0](LICENSE).

---

## 🧩 Part of the Sarto Ecosystem

This library is a standalone brick in the Sarto ecosystem, engineered to bring commercial-friendly, reflection-free enterprise Java to modern distributed runtimes.

* **Powered by TeaVM:** Sarto projects are built on top of the incredible
  [TeaVM Compiler](https://github.com/konsoletyper/teavm). If this library is serving your
  production pipelines, please consider supporting the core compiler technology that makes it all
  possible: 👉 [Sponsor TeaVM on GitHub](https://github.com/sponsors/konsoletyper)
* **Support Sarto:** To help fund the maintenance of these open-source, Apache 2.0-licensed
  enterprise bricks, consider backing the project:
  👉 [Sponsor Sarto](https://github.com/sponsors/cstainton)
