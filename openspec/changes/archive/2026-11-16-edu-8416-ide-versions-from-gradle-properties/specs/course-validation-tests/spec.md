# Spec Delta

## ADDED Requirements

### Requirement: Tested IDE version comes from the platform Gradle properties
The version of each IDE that course validation tests run in SHALL be derived from the IDE version declared for the
current platform in the Gradle properties: `ideaVersion` for IntelliJ IDEA Ultimate, `pycharmVersion` for PyCharm,
`clionVersion` for CLion and `riderVersion` for Rider. The tests SHALL NOT keep their own copy of these versions.

#### Scenario: Platform IDE version is updated
- **WHEN** `ideaVersion`, `pycharmVersion`, `clionVersion` or `riderVersion` is changed in the Gradle properties of the
  current platform
- **THEN** the next run of the course validation tests uses the IDE derived from the new value, without any change in
  the tests sources

#### Scenario: Another platform is selected
- **WHEN** the tests run with a different `environmentName` (for example, `262` instead of `263`)
- **THEN** the tested IDE is derived from the IDE versions declared in the Gradle properties of that platform

### Requirement: Release IDE version is used as is
When the declared IDE version, without its product code prefix, matches `\d{4}(\.\d+)+`, the tests SHALL download the
release build of exactly that version.

#### Scenario: Release version with two components
- **WHEN** the declared version is `IU-2026.3`
- **THEN** the tests run in the release build of IntelliJ IDEA Ultimate `2026.3`

#### Scenario: Release version with several components
- **WHEN** the declared version is `PY-2026.2.0.1`
- **THEN** the tests run in the release build of PyCharm `2026.2.0.1`

### Requirement: EAP IDE version is reduced to its major version
When the declared IDE version, without its product code prefix, does not match `\d{4}(\.\d+)+`, it SHALL be treated as
an EAP version, and the tests SHALL download an EAP build of the corresponding `20XY.Z` major version, regardless of
the exact build declared.

#### Scenario: EAP version in the build number form
- **WHEN** the declared version is `IU-263.7307-EAP-CANDIDATE-SNAPSHOT`
- **THEN** the tests run in an EAP build of IntelliJ IDEA Ultimate `2026.3`

#### Scenario: EAP version in the marketing version form
- **WHEN** the declared version is `RD-2026.3-EAP8-SNAPSHOT`
- **THEN** the tests run in an EAP build of Rider `2026.3`

### Requirement: Missing IDE version fails the test early
When the tests are launched without the IDE version information provided by the Gradle build (for example, from a
plain JUnit run configuration), the test SHALL fail before any IDE is downloaded, with an error that names the missing
setting.

#### Scenario: Launched outside of the Gradle test task
- **WHEN** a course validation test is started without the IDE version information
- **THEN** the test fails before downloading the IDE, and the error message names the missing system property
