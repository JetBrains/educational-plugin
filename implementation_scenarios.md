# Implementation Scenarios for Skipping the Toad Tour

Based on the analysis of the codebase, I've identified several approaches to implement properties that would allow skipping the Toad tour.
Below are the detailed scenarios with their pros and cons.

## Scenario 1: System Property

### Implementation Details
Add a system property check in the `EduUiOnboardingProjectActivity` class to skip the tour when a specific system property is set.

```kotlin
override suspend fun execute(project: Project) {
    if (!project.isEduProject()) return
    if (!project.isStudentProject()) return

    // Check if the tour should be skipped via system property
    if (System.getProperty("edu.ui.onboarding.skip")?.toBoolean() == true) {
        // Mark the tour as shown without actually showing it
        PropertiesComponent.getInstance().setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)
        return
    }

    val propertiesComponent = PropertiesComponent.getInstance()

    val shown = propertiesComponent.getBoolean(EDU_UI_ONBOARDING_TOUR_SHOWN)
    if (shown) return
    propertiesComponent.setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)

    EduUiOnboardingService.getInstance(project).startOnboarding()
}
```

### Pros
1. Simple to implement
2. Can be set via JVM arguments when launching the IDE
3. Consistent with other system properties used in the project
4. Doesn't require UI changes

### Cons
1. Requires IDE restart to change
2. Less discoverable for users
3. Not configurable through the UI

## Scenario 2: Registry Key

### Implementation Details
Add a registry key that can be used to disable the tour. This would be checked in the `EduUiOnboardingProjectActivity` class.

```kotlin
override suspend fun execute(project: Project) {
    if (!project.isEduProject()) return
    if (!project.isStudentProject()) return

    // Check if the tour should be skipped via registry
    if (Registry.`is`("edu.ui.onboarding.skip", false)) {
        // Mark the tour as shown without actually showing it
        PropertiesComponent.getInstance().setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)
        return
    }

    val propertiesComponent = PropertiesComponent.getInstance()

    val shown = propertiesComponent.getBoolean(EDU_UI_ONBOARDING_TOUR_SHOWN)
    if (shown) return
    propertiesComponent.setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)

    EduUiOnboardingService.getInstance(project).startOnboarding()
}
```

### Pros
1. Can be changed without IDE restart
2. Accessible through the Registry UI (Help -> Find Action -> "Registry...")
3. Consistent with other registry keys used in the project
4. Can have a description in the Registry UI

### Cons
1. Still not very discoverable for regular users
2. Requires knowledge of the Registry UI

## Scenario 3: Application-Level Setting with PropertiesComponent

### Implementation Details
Add an application-level setting using PropertiesComponent that can be toggled through a dedicated action or settings page.

```kotlin
// Add a constant for the property key
private const val EDU_UI_ONBOARDING_SKIP = "edu.ui.onboarding.skip"

override suspend fun execute(project: Project) {
    if (!project.isEduProject()) return
    if (!project.isStudentProject()) return

    // Check if the tour should be skipped via application-level setting
    if (PropertiesComponent.getInstance().getBoolean(EDU_UI_ONBOARDING_SKIP, false)) {
        // Mark the tour as shown without actually showing it
        PropertiesComponent.getInstance().setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)
        return
    }

    val propertiesComponent = PropertiesComponent.getInstance()

    val shown = propertiesComponent.getBoolean(EDU_UI_ONBOARDING_TOUR_SHOWN)
    if (shown) return
    propertiesComponent.setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)

    EduUiOnboardingService.getInstance(project).startOnboarding()
}
```

### Pros
1. Consistent with other settings in the project
2. Can be toggled without IDE restart
3. Can be exposed through a UI setting

### Cons
1. Requires additional UI work to make it discoverable
2. Not as flexible as registry keys for advanced users

## Scenario 4: Combination Approach

### Implementation Details
Implement both a system property and a registry key to provide maximum flexibility. The system property would take precedence over the registry key.

```kotlin
override suspend fun execute(project: Project) {
    if (!project.isEduProject()) return
    if (!project.isStudentProject()) return

    // Check if the tour should be skipped via system property (highest priority)
    val skipViaSystemProperty = System.getProperty("edu.ui.onboarding.skip")?.toBoolean() == true
    
    // Check if the tour should be skipped via registry
    val skipViaRegistry = Registry.`is`("edu.ui.onboarding.skip", false)
    
    if (skipViaSystemProperty || skipViaRegistry) {
        // Mark the tour as shown without actually showing it
        PropertiesComponent.getInstance().setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)
        return
    }

    val propertiesComponent = PropertiesComponent.getInstance()

    val shown = propertiesComponent.getBoolean(EDU_UI_ONBOARDING_TOUR_SHOWN)
    if (shown) return
    propertiesComponent.setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)

    EduUiOnboardingService.getInstance(project).startOnboarding()
}
```

### Pros
1. Provides maximum flexibility for different use cases
2. Caters to both regular users and advanced users/developers
3. Consistent with patterns used elsewhere in the project

### Cons
1. Slightly more complex implementation
2. Potential confusion about which setting takes precedence

## Recommendation

I recommend implementing **Scenario 2: Registry Key** for the following reasons:

1. It's consistent with how other configurable features are implemented in the project
2. It doesn't require IDE restart to change
3. It's accessible through the Registry UI for advanced users
4. It's a simple implementation that doesn't require UI changes

If more user-friendly configuration is desired, we could later extend this to include a UI setting that modifies the registry key.