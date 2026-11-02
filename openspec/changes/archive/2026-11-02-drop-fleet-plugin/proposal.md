# Proposal

## Why

`fleet-plugin` is an experimental prototype of JetBrains Academy for Fleet that is no longer developed. It is excluded
from the build by default, but it still keeps `fleetIntegration`-guarded wiring in the root build scripts, Fleet runtime
and private Space credential properties in `gradle.properties`, and an outdated module that nobody builds or maintains.
Removing it simplifies the build configuration and drops the dependency on the private Fleet SDK repository (EDU-9204).

## What Changes

- Remove the `fleet-plugin/` module (sources, `build.gradle.kts`, `README.md`).
- Remove the `fleetIntegration`-guarded `include("fleet-plugin")` and the Fleet-only `pluginManagement` repositories
  (`intellij-dependencies`, `teamcity-rest-client`, private `fleet-sdk` with Space credentials) from `settings.gradle.kts`.
- Remove the `project(":fleet-plugin") { version = ... }` block and its comment from the root `build.gradle.kts`.
- Remove the `fleetIntegration`, `fleetRuntime*Version`, `spaceUsername`, `spacePassword` properties from `gradle.properties`.
- Remove the `runFleet` run configuration (`.idea/runConfigurations/runFleet.xml`).
- **BREAKING** (developer-only): `-PfleetIntegration=true` and `:fleet-plugin:*` Gradle tasks no longer exist.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. The Fleet prototype has no specs, and the IntelliJ plugin behavior does not change, so the change sets
`skip_specs: true`.

## Impact

- Build scripts: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`.
- IDE configuration: `.idea/runConfigurations/runFleet.xml`.
- No impact on the IntelliJ plugin, `edu-format`, published artifacts, or CI (none of them reference Fleet).
