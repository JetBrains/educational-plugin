# Spec Delta

## Purpose

Defines how course validation integration tests prepare the real IDE in which the `validateCourse` command runs,
including which plugins are installed into it.

## ADDED Requirements

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
