# IDE Onboarding Module Coding Style Guidelines

## General Principles

The IDE Onboarding module follows the standard Kotlin coding conventions with some specific guidelines for educational plugin development. 
This document outlines the coding style practices observed in the module.

## Package Structure

- Package names use the standard Java/Kotlin naming convention: `com.jetbrains.edu.uiOnboarding`
- Subpackages are used to organize related functionality:
  - `steps`: Contains step implementations
  - `transitions`: Contains transition animations

## Class Organization

### File Structure

- Each class is typically defined in its own file
- The file name matches the class name (e.g., `EduUiOnboardingService.kt` contains the `EduUiOnboardingService` class)
- Copyright notice is included at the top of each file

### Class Structure

- Classes are organized with the following sections:
  1. Properties
  2. Initialization code (init blocks, constructors)
  3. Public methods
  4. Private methods
  5. Companion object (if needed)

## Naming Conventions

### General Naming

- Class names use PascalCase (e.g., `EduUiOnboardingService`)
- Interface names use PascalCase (e.g., `EduUiOnboardingStep`)
- Method names use camelCase (e.g., `startOnboarding()`)
- Property names use camelCase (e.g., `tourInProgress`)
- Private properties often use the `my` prefix (e.g., `myTourInProgress`)
- Constants use UPPER_SNAKE_CASE (e.g., `STEP_KEY`)

### Specific Naming Patterns

- Step classes end with "Step" (e.g., `WelcomeStep`)
- Animation classes describe the animation (e.g., `JumpDown`, `HappyJumpDown`)
- Service classes end with "Service" (e.g., `EduUiOnboardingService`)
- Component classes end with "Component" (e.g., `ZhabaComponent`)
- Data classes end with "Data" (e.g., `EduUiOnboardingStepData`)

## Code Style

### Kotlin Features

- Extension functions are used where appropriate
- Nullable types are used with safe calls (`?.`) and Elvis operator (`?:`)
- Coroutines are used for asynchronous operations
- Functional programming constructs (map, filter, etc.) are used where appropriate

### Comments and Documentation

- Comments explain "why" rather than "what"
- Complex logic includes explanatory comments
- Public APIs include KDoc comments
- TODO comments are avoided in favor of proper issue tracking

### Error Handling

- Null checks are performed where necessary
- Disposable resources are properly managed
- Coroutine exceptions are handled appropriately

## UI Component Guidelines

### Component Positioning

- UI components are positioned using `RelativePoint`
- Dimensions are calculated based on the parent component's size
- Constants are used for fixed dimensions and offsets

### Animation

- Animations are defined as sequences of steps
- Each step has a duration and start/end points
- Transitions between steps use predefined animation patterns

## Extension Points

- Extension points are defined in the companion object of interfaces
- Extension point names follow the pattern: `com.intellij.ide.eduUiOnboarding.step`
- Extension implementations are registered through the IntelliJ Platform extension mechanism

## Testing

- Tests follow the given-when-then pattern
- Test methods use backtick names for readability (e.g., `` `test onboarding service` ``)
- Mock objects are used to isolate the code being tested
- Tests verify both success and failure scenarios

## Code Reuse

- Common functionality is extracted into base classes or utility methods
- Animation data is loaded from resources rather than hardcoded
- Localized strings are defined in bundle files
- Constants are used for repeated values

## Platform Compatibility

- Platform-specific code is clearly marked with comments
- When using deprecated APIs, a `// BACKCOMPAT: %platform.version%` comment is added
- The code minimizes dependencies on internal platform APIs