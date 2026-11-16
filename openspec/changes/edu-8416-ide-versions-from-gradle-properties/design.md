# Design

## Context

- `ideInfoUtils.kt` (common `testSrc`) builds `IdeInfo` for `ideaUltimate()` and `pyCharm()` from
  `IdeaUltimateVersionInfo` / `PyCharmVersionInfo`. These objects live in
  `branches/{262,263}/testSrc/.../ideVersionConstants.kt` and are maintained by hand.
- `gradle-<environmentName>.properties` declare `ideaVersion`, `pycharmVersion`, `clionVersion` and `riderVersion` as
  `<product code>-<version>`. They are already exposed in `buildSrc` as `Project.ideaVersion`, `Project.pycharmVersion`,
  `Project.clionVersion` and `Project.riderVersion`. `Project.platformVersion` gives the
  `20XY.Z` form of `environmentName` (for example, `263` -> `2026.3`).
- Historical values of these properties show EAP versions as `263.7307-EAP-CANDIDATE-SNAPSHOT`,
  `263.7307.52-EAP-CANDIDATE`, `2026.3-EAP8-SNAPSHOT` (Rider), and once `LATEST-TRUNK-SNAPSHOT`. Release versions are
  always `20XY.Z[.N...]`.
- IntelliJ Starter provides `IdeInfo.CLion` and `IdeInfo.Rider` (modules `ide-starter-product-clion` /
  `ide-starter-product-rider`) in the same way as `IdeInfo.IdeaUltimate` and `IdeInfo.PyCharm`. No tests use them yet.
- The integration tests `test` task already passes `path.to.build.plugin` to the test JVM via `systemProperty`.

## Goals / Non-Goals

**Goals:**
- Single source of truth for the tested IDE versions: the platform Gradle properties.
- Keep the conversion rules in one place, on the Gradle side, as the user requested.

**Non-Goals:**
- Resolving the exact EAP build from the properties into an IntelliJ Starter build number.
- Adding CLion- or Rider-based tests. The change only makes their versions available.

## Decisions

### Conversion happens in `integration-tests/build.gradle.kts`

The build script computes `(version, buildType)` for each of `ideaVersion`, `pycharmVersion`, `clionVersion` and
`riderVersion` and passes them as system
properties of the `test` task. The test side only reads them.

- Alternative: pass raw `*Version` property values and convert in Kotlin test code. Rejected: the conversion
  depends on build conventions (`environmentName`, property format), which belong to the build.
- The conversion is a small private function in the module build script. No new `buildSrc` API, since nothing else
  needs it.

### System properties

| Property                              | Example value |
|---------------------------------------|---------------|
| `edu.integration.tests.IU.version`    | `2026.3`      |
| `edu.integration.tests.IU.build.type` | `RELEASE`     |
| `edu.integration.tests.PY.version`    | `2026.3`      |
| `edu.integration.tests.PY.build.type` | `EAP`         |
| `edu.integration.tests.CL.version`    | `2026.3.0.1`  |
| `edu.integration.tests.CL.build.type` | `RELEASE`     |
| `edu.integration.tests.RD.version`    | `2026.3`      |
| `edu.integration.tests.RD.build.type` | `RELEASE`     |

Properties are keyed by the product code taken from the property value (the part before `-`, like `IU` in
`IU-2026.3`). It is the same code as `IdeInfo.productCode` of IntelliJ Starter (`IU`, `PY`, `CL`, `RD`), so the test
side builds the property name from the `IdeInfo` itself, and neither side needs its own list of IDE names. The build
script only lists which properties to pass (`ideaVersion`, `pycharmVersion`, `clionVersion`, `riderVersion`).

- Alternative: hand-written keys (`idea`, `pycharm`, ...) on both sides. Rejected: it duplicates the information already
  present in the version string and has to be kept in sync manually.
- If the codes ever diverge, the test fails with `System property 'edu.integration.tests.<code>.version' is not set`
  instead of silently using a wrong version.
- The product code is kept in upper case, as in the Gradle properties and in `IdeInfo.productCode`.

- Build type values are `BuildType` enum names from IntelliJ Starter (`RELEASE`, `EAP`). The test side converts them
  with `BuildType.valueOf(...)` and uses `.type` for `IdeInfo.buildType`, as today. An unexpected value fails loudly
  instead of silently passing an unknown string to Starter.
- The `edu.integration.tests.` prefix scopes the properties to this module and avoids clashes with `idea.*` system properties that the platform and Starter read.

### Release detection

The version part is the property value after the product code (`substringAfter('-')`). It is a release if it fully matches `\d{4}(\.\d+)+`, and it is
then passed unchanged with `RELEASE`.

### EAP major version is `platformVersion`

For a non-release version the passed version is `Project.platformVersion` (`2026.3` for `environmentName=263`), with
`EAP`.

- Alternative: parse the major version from the property value (`263.xxxx...` -> `2026.3`, `2026.3-EAP...` -> `2026.3`).
  Rejected: it needs two parsing branches and still fails on forms like `LATEST-TRUNK-SNAPSHOT`, while
  `gradle-<environmentName>.properties` always declares IDE versions of the `environmentName` platform, so the result
  is the same.

### Remove per-platform constants

Both `branches/{262,263}/.../ideVersionConstants.kt` files are deleted, and `IdeaUltimateVersionInfo` /
`PyCharmVersionInfo` go away. `ideaUltimate()` / `pyCharm()` keep their signatures, so call sites do not change.
New `clion()` / `rider()` functions are built the same way from `IdeInfo.CLion` / `IdeInfo.Rider`. The
`integration-tests/branches` directory becomes empty and is removed. Source sets pointing to a missing
`branches/<env>/testSrc` are fine for Gradle.

### Missing property handling

A small helper in `ideInfoUtils.kt` reads a required system property and fails with
`System property '<name>' is not set`, like `resolvePluginPath()` in `CourseValidationTestBase`. The `IdeInfo`
functions are called when the IDE is set up, before any download, so the test fails early.

## Risks / Trade-offs

- [EAP runs use the latest EAP of the major version, not the declared build] -> Accepted simplification (see
  proposal). Once the properties switch to a release, as in `6329ab4caf`, tests use exactly the declared version.
- [Some release version exists in the properties but is not yet published for Starter download] -> Same as today with
  hand-written constants. The test fails on download with a clear Starter error.
- [Running a test from a plain JUnit run configuration] -> It already requires `path.to.build.plugin`. Running via the
  Gradle task is the documented way (README).
