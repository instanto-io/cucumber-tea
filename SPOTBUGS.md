# SpotBugs reports

SpotBugs runs automatically in `process-classes`, after Java compilation and
before tests, and therefore also during `package`, `verify`, `install`, and
`deploy`. A `compile`-only invocation stops before this phase.

Run the default reactor locally without running tests:

```sh
./mvnw -B -ntp --fail-at-end -DskipTests process-classes
```

Use `mvn` if this checkout has no Maven wrapper. Use the same Maven settings,
credentials and profiles as your normal build. Optional/profile-only reactors
must be selected explicitly with their usual `-P` or `-f` arguments.

Each Java module writes its own reports under `target/spotbugs/`:

- `${project.artifactId}-spotbugs.xml`: native SpotBugs findings.
- `${project.artifactId}-spotbugs.sarif`: findings for SARIF viewers.
- `${project.artifactId}-spotbugs.html`: readable HTML report.

A small Ant execution names the plugin-generated HTML report after the artifact.

Findings do not fail the build (the `check` goal is not bound). Analyzer errors
still fail it. POM-only modules and modules without compiled Java classes have
no bytecode to analyze. Reports are build outputs and are removed by `clean`.
To skip analysis explicitly, use `-Dspotbugs.skip=true`.

## TeaVM shim findings

The Unicode correctness fixes are covered by `UnicodeShimTest`, run through
TeaVM in Chrome. Run the shim tests and refresh its report with:

```sh
mvn -pl teavm-classlib-support test
```

`teavm-classlib-support/spotbugs-exclude.xml` excludes only
`BC_IMPOSSIBLE_CAST` in the `TString.replace(TCharSequence,TCharSequence)`,
`toString()` and `formatted(Object[])` methods. TeaVM maps `TString` to the
Java String API; browser tests cover both flagged replacement paths, string
identity and formatting. These four warnings are documented runtime-specific
false positives, not four corrected casts. Other cast warnings remain enabled.

The self-comparison in title-case conversion, surrogate counting and traversal
boundaries, and starting-index validation have been fixed. Binary-search
midpoints also avoid addition overflow. The remaining shim findings concern
two lazy mapping caches and an apparently unused runtime allocation hook; they
are not suppressed.
