# course-validation-tests Specification

## Purpose

Defines how course validation integration tests prepare the real IDE in which the `validateCourse` command runs,
including which plugins are installed into it.

## Requirements

### Requirement: Test declares required plugins by id
A course validation test SHALL be able to declare, by Marketplace plugin id, the plugins that must be installed into
the tested IDE. The declaration SHALL be repeatable and SHALL be allowed on a test method and on a test class.

#### Scenario: Plugin declared on a test method
- **WHEN** a test method declares a required plugin id
- **THEN** the plugin with that id is installed into the IDE launched for this test method before the IDE starts

#### Scenario: Plugin declared on a test class
- **WHEN** a test class declares a required plugin id
- **THEN** the plugin is installed into the IDE launched for every test method of that class

#### Scenario: Plugin declared on a base test class
- **WHEN** an abstract base test class declares a required plugin id and a concrete test class extends it
- **THEN** the plugin is installed into the IDE launched for every test method of the concrete class

#### Scenario: Several plugins declared
- **WHEN** several required plugin ids are declared across the test method, its class and the base classes
- **THEN** every declared plugin is installed into the tested IDE

#### Scenario: Same plugin declared several times
- **WHEN** the same plugin id is declared more than once for a test (for example, on the class and on the method)
- **THEN** the plugin is installed only once

### Requirement: Required plugins match the tested IDE
Each required plugin SHALL be downloaded from the JetBrains Marketplace in the latest version compatible with the build
of the tested IDE.

#### Scenario: Compatible version is available
- **WHEN** a required plugin has a version compatible with the tested IDE build
- **THEN** that version is installed and the IDE starts with the plugin enabled

#### Scenario: Plugin is not available for the IDE build
- **WHEN** a required plugin id does not exist or has no version compatible with the tested IDE build
- **THEN** the test fails before the IDE is launched, with an error that names the plugin id

### Requirement: Required properties are inherited by test subclasses
A required property declared on a test class SHALL also be required for all test classes extending it, in the same way
as a required plugin.

#### Scenario: Property declared on a base test class
- **WHEN** an abstract base test class declares a required property and a concrete test class extends it
- **THEN** the property value is required and passed into the IDE launched for every test method of the concrete class

### Requirement: Built plugin is always installed
The built JetBrains Academy plugin SHALL be installed into the tested IDE for every test, whether or not the test
declares required plugins.

#### Scenario: No plugins declared
- **WHEN** neither the test method nor any of its classes declares a required plugin
- **THEN** only the built JetBrains Academy plugin is installed into the tested IDE, as before

#### Scenario: Plugins declared
- **WHEN** a test declares required plugins
- **THEN** both the required plugins and the built JetBrains Academy plugin are installed into the tested IDE

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
