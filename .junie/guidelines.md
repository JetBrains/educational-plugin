# Educational Plugin Development Guidelines

This document provides essential information for developers working on the Educational Plugin project. It includes build/configuration instructions, testing information, and additional development guidelines.

## Build/Configuration Instructions

### Environment Setup
1. **Java Requirements**: Java 17 is required for development.
2. **IDE**: Open the project in IntelliJ IDEA (Community Edition is sufficient).
3. **Gradle**: The project uses Gradle for building. Import it as a Gradle project.

### Build Configuration
1. **Platform Versions**: The project supports multiple IntelliJ Platform versions. The current platform version is configured in `gradle.properties` via the `environmentName` property.
2. **Building the Plugin**:
   - Use the `:buildPlugin` Gradle task to build the plugin distribution.
   - The built plugin will be available in the `build/distributions` directory.
   - Install it via `Settings > Plugins > Install plugin from disk...`.

### Running the Plugin
- Predefined run configurations are available for different IDEs:
  - `runIdea` - for IntelliJ IDEA
  - `runPyCharm` - for PyCharm
  - `runCLion` - for CLion
  - `runStudio` - for Android Studio
  - `runWebStorm` - for WebStorm
  - `runGoLand` - for GoLand

### Project Structure
- The project is a multi-module Gradle project with the following main modules:
  - `edu-format`: Core educational format definitions
  - `intellij-plugin`: The main plugin module with submodules for different languages and features
  - `fleet-plugin`: Optional Fleet IDE integration

## Testing Information

### Test Structure
1. **Test Directories**: Tests are located in `testSrc` directories within each module.
2. **Test Resources**: Test resources are in `testResources` directories.

### Test Base Classes
1. **EduTestCase**: The base class for most tests, extending BasePlatformTestCase from the IntelliJ Platform.
2. **EduActionTestCase**: For testing actions, extends EduTestCase.
3. **EduHeavyTestCase**: For tests requiring a heavier test environment, extends HeavyPlatformTestCase.

### Writing Tests
1. **Naming Convention**: Test methods use descriptive names with backticks, e.g., `` `test linkedin connector instance` ``.
2. **Pattern**: Tests follow the given-when-then pattern with clear comments.
3. **Annotations**: Use the `@Test` annotation from JUnit for test methods.

### Example Test
Here's a simple test for the LinkedInConnector class:

```kotlin
/**
 * Simple test for the LinkedInConnector class.
 * This test demonstrates how to set up and run tests in the educational plugin.
 */
class LinkedInConnectorTest : EduTestCase() {

  private lateinit var helper: MockWebServerHelper

  override fun setUp() {
    super.setUp()
    helper = MockWebServerHelper(testRootDisposable)
    // Replace the service with a mock that uses our test server
    application.replaceService(LinkedInConnector::class.java, LinkedInConnector(), testRootDisposable)
  }

  @Test
  fun `test linkedin connector instance`() {
    // given
    helper.addResponseHandler(testRootDisposable) { _, path ->
      when (path) {
        "/oauth/v2/authorization" -> MockResponseFactory.ok()
        else -> null
      }
    }

    // when
    val connector = LinkedInConnector.getInstance()

    // then
    assertNotNull(connector)
    // Test that the connector is the same instance as the one we created
    assertSame(connector, LinkedInConnector.getInstance())
  }
}
```

### Running Tests
- Use the Gradle task `:intellij-plugin:test` to run all tests.
- For running specific tests, use the appropriate Gradle task for the module, e.g., `:intellij-plugin:features:social-media:test`.
- Tests can also be run directly from the IDE by right-clicking on the test class or method and selecting "Run".

## Additional Development Information

### Platform Compatibility
1. **Multiple Platforms**: The project supports multiple IntelliJ Platform versions.
2. **Platform-Specific Code**: 
   - Platform-specific code is organized in `branches/%platform.version%/src` directories.
   - Use the `environmentName` property in `gradle.properties` to switch between platforms.
   - When adding platform-specific code, minimize the amount of code that needs to be duplicated.

### Code Style
1. **Kotlin Style**: The project follows the standard Kotlin coding conventions.
2. **Null Safety**: Be careful with null safety, especially when working with IntelliJ Platform APIs.
3. **Deprecation**: When using deprecated APIs, add a `// BACKCOMPAT: %platform.version%` comment to indicate when it should be fixed.
4. **IDE Configuration**: The code style is specified in IntelliJ IDEA's XML configuration files.

### Documentation
1. **Comments**: Use descriptive comments, especially for complex logic.
2. **KDoc**: Use KDoc for documenting classes and methods.
3. **TODO Comments**: Do not use TODO comments to mark code that needs to be improved or fixed in the future.

### Testing Best Practices
1. **Mock Dependencies**: Use mocks for external dependencies to make tests more reliable and faster.
2. **Test Edge Cases**: Make sure to test edge cases and error conditions.
3. **Test Organization**: Organize tests in a way that makes it clear what functionality is being tested.
