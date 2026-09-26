# Cucumber Tea

Cucumber Tea runs Gherkin scenarios as Java tests in TeaVM. Write a standard
`.feature` file, connect its sentences to annotated Java methods, and let the
build generate JUnit tests. The companion Gherkin Tea module uses the official
Gherkin parser; Cucumber Tea uses Cucumber Expressions to bind steps. Neither
parser nor annotation processor is part of the generated JavaScript.

The same feature can exercise a JVM implementation, a browser application, or a
Cloudflare Worker. The Java steps provide the implementation-specific fixture.
This fits Java projects using TeaVM: generated tests call step methods directly,
while the existing JUnit runner owns execution in each host.

## A first scenario

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

At test compilation, Cucumber Tea checks each sentence and generates an
ordinary JUnit class named `ArithmeticStepsTest`. Undefined or ambiguous steps
fail the build. For browser runs, `TeaVMTestRunner` compiles the test and
reachable Java code to JavaScript and runs it in Chromium. The
[executable arithmetic example](cucumber-tea-examples/src/test/java/io/instanto/cucumber/tea/examples/ArithmeticSteps.java)
also shows hooks, tables, doc strings, outlines, and host tags.

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

## Choose where scenarios run

The generator supports `teavm` (Chromium), `jvm` (ordinary JUnit), and
`miniflare` (Cloudflare Worker through Sarto Edge) targets. Use tags on
individual scenarios when a feature spans hosts:

| Tag | Effect |
| --- | --- |
| No host tag or `@portable` | Include in every generated target. |
| `@jvm`, `@browser`, `@worker` | Include only in that target. |
| `@skip-jvm` | Skip TeaVM's preliminary JVM execution for JavaScript-only code. Pair it with a host tag where appropriate. |

For JVM tests, a suite can use `contexts` to run each scenario against named
environments, or `runner` to start host infrastructure around the suite. These
two options cannot be combined. See the
[JVM examples](cucumber-tea-examples-jvm/src/test/java/io/instanto/cucumber/tea/examples/jvm).

For browser UI tests, use `@browser @skip-jvm` on DOM-only scenarios. Keep DOM
queries, events, and component mounting in a test kit or fixture, leaving the
feature focused on visible behaviour. A suite can supply JavaScript helpers
through `@CucumberScript`. The [Webapp Testkit](https://github.com/instanto-io/webapp-testkit)
provides browser-facing DOM test support.

For Worker tests, Sarto Edge's runner supplies the Miniflare host. Mark
Worker-only scenarios `@worker @skip-jvm`; Cucumber Tea generates the same
style of JUnit test. The
[organised example](cucumber-tea-examples-organisation/README.md) shows how
portable and host-specific scenarios can share a feature tree.

## More examples

The repository contains
[small examples](cucumber-tea-examples),
[JVM contexts and a custom runner](cucumber-tea-examples-jvm), and
[a larger test-library example](cucumber-tea-examples-organisation/README.md).

## Credits and support

Gherkin Tea and Cucumber Tea are licensed under
[Apache 2.0](LICENSE). They build on [Gherkin and Cucumber
Expressions](https://github.com/cucumber) and [TeaVM](https://teavm.org/).
If they help your work, you can
[support TeaVM](https://github.com/sponsors/konsoletyper) or
[support this project's maintenance](https://github.com/sponsors/cstainton).
