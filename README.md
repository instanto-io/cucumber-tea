# Cucumber Tea

Cucumber Tea runs Gherkin scenarios as Java tests in TeaVM. Write a standard
`.feature` file, connect its sentences to annotated Java methods, and let the
build generate JUnit tests. The companion Gherkin Tea module uses the official
Gherkin parser; Cucumber Tea uses Cucumber Expressions to bind steps. Neither
parser nor annotation processor is part of the generated JavaScript.

The same feature can exercise a JVM implementation, a browser application, or a
Cloudflare Worker. The Java steps provide the implementation-specific fixture.

## Start with a browser test

You need JDK 21, Maven, and a Chromium browser available to TeaVM's test runner.
The current version is `0.1.0-SNAPSHOT`; there is no release yet. Until snapshots
are published, install this repository with `./mvnw install`. If Maven cannot
resolve `io.instanto:instanto-org-pom:0.1.0-SNAPSHOT`, first install a sibling
`instanto-poms` checkout with `mvn -f ../instanto-poms/pom.xml install`.

Create `src/test/resources/features/arithmetic.feature`:

```gherkin
Feature: Arithmetic

  Scenario: add a value
    Given the display shows 40
    When I add 2
    Then the result is 42
```

Connect the sentences in `src/test/java/example/ArithmeticSteps.java`:

```java
package example;

import static org.junit.Assert.assertEquals;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Given;
import io.instanto.cucumber.tea.Then;
import io.instanto.cucumber.tea.When;

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

Add the test runtime and TeaVM runner to your module:

```xml
<properties>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <maven.compiler.release>21</maven.compiler.release>
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
  <dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Configure test compilation and browser execution:

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
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>3.6.0-M1</version>
      <configuration>
        <systemPropertyVariables>
          <teavm.junit.target>${project.build.directory}/teavm-tests</teavm.junit.target>
          <teavm.junit.js.runner>browser-chrome</teavm.junit.js.runner>
        </systemPropertyVariables>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Run `mvn test`. During `testCompile`, the processor checks every sentence and
generates `ArithmeticStepsTest`. Undefined or ambiguous steps fail compilation.
`TeaVMTestRunner` then compiles the test and reachable application code to
JavaScript and runs the scenario in Chromium. The
[executable arithmetic example](cucumber-tea-examples/src/test/java/io/instanto/cucumber/tea/examples/ArithmeticSteps.java)
also demonstrates hooks, tables, doc strings, outlines, and host tags.

Keep `cucumber-tea-codegen` on the annotation-processor path in a consuming
project. The example modules use a test dependency as well because they build
the generator and examples in one Maven reactor.

## Grow the feature

Cucumber Expressions pass values such as `{int}` and `{string}` to step methods.
A Scenario Outline runs once for each Examples row:

```gherkin
Scenario Outline: add a value
  Given the display shows <start>
  When I add <amount>
  Then the result is <result>

  Examples:
    | start | amount | result |
    | 40    | 2      | 42     |
    | 10    | 5      | 15     |
```

A Gherkin table becomes an immutable `DataTable` argument; a doc string becomes
a `String` argument. Each scenario receives a new step-class instance. Use
`@BeforeScenario` and `@AfterScenario` for fixture setup and cleanup, and a
Gherkin `Background` for starting conditions the reader should see. After hooks
are attempted even when a step fails.

```java
@BeforeScenario
public void openFixture() {
    fixture = new CalculatorFixture();
}

@AfterScenario
public void closeFixture() {
    fixture.close();
}
```

## Choose where scenarios run

Set `-Acucumber.tea.runner` in the compiler arguments to `teavm` (Chromium),
`jvm` (ordinary JUnit), or `miniflare` (Cloudflare Worker through Sarto Edge).
Use tags on individual scenarios when a feature spans hosts:

| Tag | Effect |
| --- | --- |
| No host tag or `@portable` | Include in every generated target. |
| `@jvm`, `@browser`, `@worker` | Include only in that target. |
| `@skip-jvm` | Skip TeaVM's preliminary JVM execution for JavaScript-only code. Pair it with a host tag where appropriate. |

For JVM tests, change the processor argument to
`-Acucumber.tea.runner=jvm`. A suite can use `contexts` to run each scenario
against named JVM environments, or `runner` to start host infrastructure around
the suite. These two options cannot be combined. See the
[JVM examples](cucumber-tea-examples-jvm/src/test/java/io/instanto/cucumber/tea/examples/jvm).

For browser UI tests, use `@browser @skip-jvm` on DOM-only scenarios. Keep DOM
queries, events, and component mounting in a test kit or fixture, leaving the
feature focused on visible behaviour. A suite can supply JavaScript helpers
through `@CucumberScript`. The [Webapp Testkit](https://github.com/instanto-io/webapp-testkit)
provides browser-facing DOM test support.

For Worker tests, select `-Acucumber.tea.runner=miniflare` and add
`io.instanto:sarto-edge-cf-junit` as a test dependency. Mark Worker-only
scenarios `@worker @skip-jvm`. Sarto Edge's runner supplies the Miniflare host;
Cucumber Tea still generates the same style of JUnit test. The
[Miniflare profile](cucumber-tea-examples/pom.xml) shows the dependency and
runner configuration. The
[organised example](cucumber-tea-examples-organisation/README.md) shows how
portable and host-specific scenarios can share a feature tree.

## Build and support

Run the browser examples in this repository with `./mvnw clean test`. When the
Sarto Edge runner and its local Worker environment are available, run
`./mvnw clean test -Pminiflare`. The repository contains
[small examples](cucumber-tea-examples),
[JVM contexts and a custom runner](cucumber-tea-examples-jvm), and
[a larger test-library example](cucumber-tea-examples-organisation/README.md).

Gherkin Tea and Cucumber Tea are licensed under
[Apache 2.0](LICENSE). They build on [Gherkin and Cucumber
Expressions](https://github.com/cucumber) and [TeaVM](https://teavm.org/).
If they help your work, you can
[support TeaVM](https://github.com/sponsors/konsoletyper) or
[support this project's maintenance](https://github.com/sponsors/cstainton).
