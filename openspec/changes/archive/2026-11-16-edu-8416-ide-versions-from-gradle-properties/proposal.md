# Proposal

## Why

Course validation integration tests (EDU-8416) download the IDE they run in by the version and build type hardcoded
in `branches/<platform>/testSrc/.../ideVersionConstants.kt`. The same IDE versions are already declared in
`gradle-<platform>.properties` (`ideaVersion`, `pycharmVersion`, `clionVersion`, `riderVersion`), so every platform update has to change both places,
and it is easy to forget the test constants. For example, after `6329ab4caf` switched `gradle-263.properties` from EAP
builds to the `2026.3` release, the tests still download a `2026.3` EAP build.

## What Changes

- The `test` task of `intellij-plugin/integration-tests` reads `ideaVersion`, `pycharmVersion`, `clionVersion` and
  `riderVersion` from the Gradle properties of the current platform, converts each one into a version and build type understood by IntelliJ
  Starter, and passes them to the test JVM as system properties, the same way it passes `path.to.build.plugin`.
- Conversion rules:
  - a release version (the part after the product code matches `\d{4}(\.\d+)+`, e.g. `IU-2026.3`, `CL-2026.3.0.1`) is
    passed as is with the `release` build type;
  - any other version is an EAP one (e.g. `IU-263.7307-EAP-CANDIDATE-SNAPSHOT`, `RD-2026.3-EAP8-SNAPSHOT`), and only its
    major version in the `20XY.Z` form (e.g. `2026.3`) is passed with the `eap` build type. The tests then get the
    latest EAP build of that major version instead of the exact build from the properties. This is a known
    simplification.
- The test side builds `IdeInfo` for IntelliJ IDEA Ultimate, PyCharm, CLion and Rider from these system properties and
  fails with a clear message if they are not set. CLion and Rider get new `clion()` / `rider()` functions next to the
  existing `ideaUltimate()` / `pyCharm()`, so future CLion- and Rider-based tests use the same versions as the build.
- **BREAKING** (test-only): `branches/262` and `branches/263` copies of `ideVersionConstants.kt` with
  `IdeaUltimateVersionInfo` / `PyCharmVersionInfo` are removed. Platform updates no longer touch the integration
  tests sources.
- `intellij-plugin/integration-tests/README.md` describes where the tested IDE version comes from.

Out of scope:
- downloading the exact EAP build declared in the properties;
- adding CLion- or Rider-based course validation tests.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `course-validation-tests`: adds a requirement on which IDE version and build type the tests download, derived from
  the platform Gradle properties.

## Impact

- `intellij-plugin/integration-tests/build.gradle.kts`: computes and passes the new system properties.
- `intellij-plugin/integration-tests/testSrc/com/jetbrains/edu/slow/checkerTests/ideInfoUtils.kt`: reads them.
- Removed: `intellij-plugin/integration-tests/branches/{262,263}/testSrc/com/jetbrains/edu/slow/checkerTests/ideVersionConstants.kt`.
- `intellij-plugin/integration-tests/README.md`.
- Running an integration test outside Gradle (for example, a plain JUnit run configuration) needs these system
  properties set manually, the same as `path.to.build.plugin` today.
- No production code changes.
