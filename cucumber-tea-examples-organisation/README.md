# Organise a larger test library

This executable example shows how to keep Gherkin features readable as a test
library grows. Read the [Cucumber Tea guide](../README.md) for setup and the
first test; this module focuses on arranging several related features.

## Group by application behaviour

The feature tree names capabilities. Java packages follow the same boundaries:

```text
src/test/resources/features/
  commerce/
    customer-accounts.feature
    order-placement.feature
  platform/
    worker-health.feature

src/test/java/io/instanto/cucumber/tea/examples/organisation/
  commerce/CommerceIntegrationSteps.java
  platform/WorkerPlatformSteps.java
  support/
    OrganisedIntegrationSteps.java
    ScenarioWorld.java
    HostGlobals.java
```

[Order placement](src/test/resources/features/commerce/order-placement.feature)
uses ordinary domain language and several examples:

```gherkin
Scenario Outline: retain the order quantity
  Given a registered customer "<customer>"
  When "<customer>" places an order for <quantity> items
  Then the order for "<customer>" contains <quantity> items

  Examples:
    | customer | quantity |
    | Ada      | 2        |
    | Grace    | 5        |
```

[`CommerceIntegrationSteps`](src/test/java/io/instanto/cucumber/tea/examples/organisation/commerce/CommerceIntegrationSteps.java)
owns the vocabulary shared by accounts and orders. A suite can name more than
one feature when they use the same fixture and step language:

```java
@CucumberSuite({
    "features/commerce/customer-accounts.feature",
    "features/commerce/order-placement.feature"
})
public final class CommerceIntegrationSteps
        extends OrganisedIntegrationSteps {
    // Annotated commerce steps
}
```

Split a suite when another capability would need a different fixture or make
this vocabulary ambiguous.

## Keep scenarios independent

Cucumber Tea creates a new step-class instance for every scenario.
[`ScenarioWorld`](src/test/java/io/instanto/cucumber/tea/examples/organisation/support/ScenarioWorld.java)
holds that instance's customers and orders. The shared
[base class](src/test/java/io/instanto/cucumber/tea/examples/organisation/support/OrganisedIntegrationSteps.java)
starts and closes it through scenario hooks. Ada's data cannot leak into Grace's
example.

Use hooks for fixture mechanics. Use a Gherkin `Background` when a starting
condition helps the reader understand every scenario.

## Put host details at the edge

Most commerce scenarios are portable. One browser-only scenario carries
`@browser @skip-jvm`; the platform feature uses `@worker @skip-jvm`.
[`HostGlobals`](src/test/java/io/instanto/cucumber/tea/examples/organisation/support/HostGlobals.java)
keeps the small JavaScript probe out of the feature text. In an application,
the same boundary might be a DOM test kit or a Worker request fixture.

Organise a UI suite in the same way: group features by user capability, keep
scenario state local, and hide selectors and rendering mechanics in fixtures.
Tags select the necessary host without duplicating the portable scenarios.

## Run it

From the repository root, run the Chromium tests:

```sh
./mvnw -pl cucumber-tea-examples-organisation -am clean test
```

When Sarto Edge and its local Worker environment are available, run the Worker
tests:

```sh
./mvnw -pl cucumber-tea-examples-organisation -am clean test -Pminiflare
```

Use `clean` when switching targets because the processor generates different
JUnit source for each host.
