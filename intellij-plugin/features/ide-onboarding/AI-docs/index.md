# IDE Onboarding Module Documentation

Welcome to the documentation for the IDE Onboarding module of the Educational Plugin. This documentation provides comprehensive information about the module's purpose, structure, behavior, dependencies, and coding style.

## Table of Contents

1. [Overview](overview.md) - Introduction to the module and its purpose
2. [File Structure](file_structure.md) - Documentation of the module's structure and key classes
3. [Feature Behavior](feature_behavior.md) - Description of the module's features and their behavior
4. [Dependencies](dependencies.md) - Information about the module's dependencies
5. [Coding Style](coding_style.md) - Guidelines for coding style specific to the module

## Purpose

The IDE Onboarding module provides an interactive, guided tour of the Educational Plugin's features for new users. 
It helps users understand how to navigate the IDE, read task descriptions, work with the code editor, check solutions, and use the course view.

## Getting Started

To start the onboarding process programmatically, use:

```kotlin
EduUiOnboardingService.getInstance(project).startOnboarding()
```

The onboarding can also be started by the user through the `StartEduUiOnboardingAction` or automatically when a project is opened.

## Contributing

When contributing to the IDE Onboarding module, please follow the [coding style guidelines](coding_style.md) and ensure that your changes maintain the [expected behavior](feature_behavior.md) of the features.

New onboarding steps can be added by implementing the `EduUiOnboardingStep` interface and registering the implementation through the extension point.

## Testing

The module can be tested by running the Educational Plugin with a sample course and verifying that the onboarding process works as expected. Unit tests can also be written to test specific components of the module.