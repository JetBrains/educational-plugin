# IDE Onboarding Module Dependencies

## Build Dependencies

The IDE Onboarding module has the following build dependencies as defined in `build.gradle.kts`:

```kotlin
dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
```

### IntelliJ Platform Dependencies

- **intellijIde(baseVersion)**: Core IntelliJ Platform APIs
  - Used for UI components, services, and extensions
  - The specific version is determined by the `baseVersion` property

### Internal Project Dependencies

- **:intellij-plugin:educational-core**: The core module of the Educational Plugin
  - Provides essential educational functionality
  - Contains course model, task management, and other core features

### Test Dependencies

- **:intellij-plugin:educational-core (testOutput)**: Test utilities from the educational-core module
  - Used for testing the onboarding functionality
  - Provides test fixtures and helper methods

## IntelliJ Platform API Dependencies

The module uses the following IntelliJ Platform APIs:

### UI Components

- **com.intellij.openapi.ui.popup.Balloon**: For displaying tooltips
- **com.intellij.ui.GotItComponentBuilder**: For building "Got It" tooltips
- **com.intellij.ui.awt.RelativePoint**: For positioning UI elements

### Project and Application Services

- **com.intellij.openapi.components.Service**: For defining services
- **com.intellij.openapi.components.service**: For accessing services
- **com.intellij.openapi.project.Project**: For project-level operations

### Tool Windows

- **com.intellij.openapi.wm.ToolWindowManager**: For managing tool windows
- **com.intellij.openapi.wm.ToolWindowId**: For referencing standard tool windows

### Notifications

- **com.intellij.notification.NotificationGroupManager**: For showing notifications

### Disposable Management

- **com.intellij.openapi.Disposable**: For resource cleanup
- **com.intellij.openapi.util.Disposer**: For managing disposable resources

### Extensions

- **com.intellij.openapi.extensions.ExtensionPointName**: For defining extension points
- **com.intellij.util.KeyedLazyInstanceEP**: For lazy-loaded extension points

## Educational Plugin Dependencies

The module uses the following components from the Educational Plugin:

### Statistics

- **com.jetbrains.edu.learning.statistics.EduCounterUsageCollector**: For collecting usage statistics
  - Tracks onboarding starts, completions, skips, and relaunches

## Third-Party Dependencies

### Kotlin Coroutines

- **kotlinx.coroutines.CoroutineScope**: For managing coroutine scopes
- **kotlinx.coroutines.Dispatchers**: For specifying coroutine dispatchers
- **kotlinx.coroutines.launch**: For launching coroutines

### Java Standard Library

- **java.util.concurrent.atomic.AtomicBoolean**: For thread-safe boolean operations
- **java.awt.Point**: For representing coordinates
- **javax.swing.JComponent**: For Swing UI components
- **javax.swing.JLayeredPane**: For layered UI components