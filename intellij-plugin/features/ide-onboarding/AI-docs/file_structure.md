# IDE Onboarding Module File Structure

## Package Structure

The IDE Onboarding module is organized in the following package structure:

```
com.jetbrains.edu.uiOnboarding
├── steps/           # Implementations of specific onboarding steps
└── transitions/     # Animations for transitions between steps
```

## Core Classes

### Service and Execution

- **EduUiOnboardingService**: Project-level service that manages the onboarding process
  - Responsible for starting and finishing the onboarding
  - Maintains the state of the onboarding process
  - Defines the default order of steps

- **EduUiOnboardingExecutor**: Executes the onboarding process
  - Runs each step in sequence
  - Handles transitions between steps
  - Manages user interactions (next, skip, finish, restart)
  - Collects usage statistics

### Step Definition

- **EduUiOnboardingStep**: Interface that defines the contract for onboarding steps
  - `performStep()`: Performs the actual step
  - `isAvailable()`: Checks if the step is available
  - `buildAnimation()`: Builds the animation for the step
  - `createZhaba()`: Creates a zhaba (frog) component for the step

- **EduUiOnboardingStepData**: Data class that holds information about a step
  - Contains the tooltip builder, tooltip position, zhaba position, etc.

### Animation

- **EduUiOnboardingAnimation**: Interface for animations
  - Defines a list of animation steps and whether the animation should cycle

- **EduUiOnboardingAnimationStep**: Represents a single step in an animation
  - Contains the animation data, start and end points, and duration

- **EduUiOnboardingAnimationData**: Contains the animation data (images, dimensions, etc.)
  - Loaded from resources

- **ZhabaComponent**: UI component that displays the animated zhaba (frog)
  - Renders the animation
  - Handles animation timing

### Transitions

- **JumpDown**: Basic jump down transition
- **HappyJumpDown**: Jump down with happy animation
- **SadJumpDown**: Jump down with sad animation
- **JumpLeft**: Jump left transition
- **JumpRight**: Jump right transition

## Step Implementations

- **WelcomeStep**: Initial welcome step
  - Shows a welcome message in the Project tool window
  - Introduces the zhaba character

- **TaskDescriptionStep**: Explains how to read task descriptions
  - Highlights the task description panel

- **CodeEditorStep**: Introduces the code editor
  - Shows how to work with code

- **CheckSolutionStep**: Demonstrates how to check solutions
  - Highlights the check solution button

- **CourseViewStep**: Introduces the course view
  - Shows how to navigate the course structure

## Resources

- **Icons and Images**: Located in the resources directory
  - Zhaba character animations
  - UI icons

- **Localized Messages**: Defined in EduUiOnboardingBundle.kt
  - Text for tooltips, buttons, and other UI elements