# Organising Cucumber Tea integration tests

This module is an executable example of a test library after it grows beyond
one feature and one step class. It shows where to put readable behaviour,
application-facing test vocabulary, scenario state, and host-specific helpers.

Organise tests around what the application does, not around the tool that
renders it or the host that executes it.

## The example

The small example has two application areas:

- `commerce` covers customer accounts and order placement;
- `platform` covers behaviour that exists only inside a Cloudflare Worker.

Most commerce scenarios can run in Chromium or Miniflare. One checkout
scenario needs the browser, while the platform scenario needs a Worker. Tags
express those local differences without creating parallel browser and Worker
copies of the feature tree.

```text
src/test/resources/features/
  commerce/
    customer-accounts.feature
    order-placement.feature
  platform/
    worker-health.feature

src/test/java/io/instanto/cucumber/tea/examples/organisation/
  commerce/
    CommerceIntegrationSteps.java
  platform/
    WorkerPlatformSteps.java
  support/
    OrganisedIntegrationSteps.java
    ScenarioWorld.java
    HostGlobals.java
```

Feature folders name capabilities which a reader recognises. The Java packages
mirror those capabilities, while `support` holds mechanics shared by more than
one suite.

## Start with behaviour

The order feature describes the outcome and supplies several examples:

```gherkin
Feature: Order placement

  Background:
    Given an isolated integration scenario

  @portable
  Scenario Outline: retain the order quantity
    Given a registered customer "<customer>"
    When "<customer>" places an order for <quantity> items
    Then the order for "<customer>" contains <quantity> items

    Examples:
      | customer | quantity |
      | Ada      | 2        |
      | Grace    | 5        |
```

The feature does not say how a customer is stored or how a test host starts.
It can therefore survive a change of implementation. The two examples become
independent generated JUnit tests.

## Give related features one vocabulary

Customer accounts and order placement use the same domain words, so one step
class owns both feature resources:

```java
@CucumberSuite({
    "features/commerce/customer-accounts.feature",
    "features/commerce/order-placement.feature"
})
public final class CommerceIntegrationSteps
        extends OrganisedIntegrationSteps {

    @Given("a registered customer {string}")
    public void registeredCustomer(String name) {
        world.registerCustomer(name);
    }

    @When("{string} places an order for {int} items")
    public void placeOrder(String customer, int quantity) {
        world.placeOrder(customer, quantity);
    }

    @Then("the order for {string} contains {int} items")
    public void orderContains(String customer, int quantity) {
        assertEquals(quantity, world.orderQuantity(customer));
    }
}
```

A suite boundary is a vocabulary and fixture boundary. It need not correspond
to exactly one feature file. Split a suite when another capability uses
different collaborators or its sentences would make the existing step
vocabulary confusing or ambiguous.

## Keep each scenario independent

Cucumber Tea creates a new step-class object for every scenario. This example
keeps its mutable customers and orders in a `ScenarioWorld` owned by that
object. Data created for Ada in one example cannot leak into Grace's example.

The base class shares preparation and cleanup without making the state static:

```java
public abstract class OrganisedIntegrationSteps {
    protected final ScenarioWorld world = new ScenarioWorld();

    @BeforeScenario
    public void beginScenario() {
        world.begin();
    }

    @AfterScenario
    public void finishScenario() {
        world.finish();
    }
}
```

Use hooks for technical preparation and cleanup. Use a Gherkin `Background`
when the starting condition is part of understanding the behaviour. A UI suite
might mount and unmount its component in hooks, while its Background says that
a signed-in user has opened the weekly timesheet.

## Keep browser and Worker details at the edge

Put a target tag on the individual scenario which needs it:

```gherkin
@browser @skip-jvm
Scenario: render checkout in its browser host
  Then checkout is executing in a browser
```

The rest of the commerce feature stays portable. `HostGlobals` contains the
small TeaVM `@JSBody` probe used by this demonstration; the step class sees a
plain Java method. In a real application the same role may be filled by a UI
test kit, a Worker request fixture, or a binding wrapper.

The Worker profile uses Sarto Edge's `MiniflareTestRunner`. It compiles the
generated JUnit test with TeaVM and executes that JavaScript inside the
Miniflare development environment, where Cloudflare Worker APIs and bindings
are available. Cucumber Tea itself remains independent of Sarto Edge.

## Apply the structure to a UI library

For a larger UI test suite:

1. Group features by user capability, such as `timesheets`, `search`, or
   `account-settings`.
2. Let step classes express the vocabulary of those capabilities.
3. Give each scenario a fresh application fixture and DOM root.
4. Put selectors, events, mounting, and rendering details behind the UI test
   kit rather than in the feature.
5. Put `@browser @skip-jvm` only on scenarios which genuinely require the DOM.

The same feature can be exercised against different UI implementations. A
React step class can use TeaVM React's test kit and Mockatcha BDD DOM; a Verrai
step class can use the existing Verrai UI test kit. They share the behaviour
but keep the implementation-specific operations in their respective glue.

## What each file solves

| File | Responsibility |
| --- | --- |
| `customer-accounts.feature` | Customer behaviour a reader can review. |
| `order-placement.feature` | Ordering behaviour and its input examples. |
| `worker-health.feature` | A capability which exists only in the Worker host. |
| `CommerceIntegrationSteps` | Java vocabulary and collaborators for commerce. |
| `WorkerPlatformSteps` | Java vocabulary for the Worker platform capability. |
| `OrganisedIntegrationSteps` | Shared preparation and cleanup. |
| `ScenarioWorld` | Mutable state belonging to one scenario. |
| `HostGlobals` | Host-specific implementation detail kept out of the feature. |

`ScenarioWorld` and `HostGlobals` are example support classes, not Cucumber
Tea APIs. A project should name and shape its own helpers around its domain.

## Run the example

From the repository root, run the Chromium version with:

```shell
./mvnw -pl cucumber-tea-examples-organisation -am clean test
```

When the Sarto Edge runner and development container are installed locally,
run the Worker version with:

```shell
./mvnw -pl cucumber-tea-examples-organisation -am clean test -Pminiflare
```

Use `clean` when changing profiles because the selected runner changes the
generated Java test source.

For dependency setup and the full API, return to the
[Gherkin Tea and Cucumber Tea guide](../README.md).
