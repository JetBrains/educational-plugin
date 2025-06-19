# IDE Onboarding Feature Behavior

## Onboarding Flow

The IDE Onboarding module provides a guided tour of the Educational Plugin's features. The flow follows these steps:

1. **Initialization**:
   - The onboarding is started by calling `EduUiOnboardingService.startOnboarding()`
   - This can be triggered by the `StartEduUiOnboardingAction` or automatically when a project is opened

2. **Step Execution**:
   - Steps are executed in sequence as defined in `EduUiOnboardingService.getDefaultStepsOrder()`
   - Each step is checked for availability before execution
   - If a step is not available, it is skipped

3. **Transitions**:
   - Smooth animations are played between steps
   - Different transitions are used based on the source and destination steps

4. **Completion**:
   - When all steps are completed, a final animation is played
   - A notification is shown to remind the user about the onboarding
   - The "Meet New UI" tool window is activated

## User Interactions

The onboarding process supports several user interactions:

1. **Navigation**:
   - **Next Button**: Proceeds to the next step
   - **Skip All Button**: Skips the entire onboarding process
   - **Finish Button**: Completes the onboarding (on the last step)
   - **Restart Button**: Restarts the onboarding from the second step (on the last step)
   - **Escape Key**: Exits the onboarding process

2. **Feedback**:
   - Different animations are played based on whether the user completes or skips the onboarding
   - A happy animation is played when the user completes the onboarding
   - A sad animation is played when the user skips the onboarding

## Step Behaviors

Each step in the onboarding process has specific behavior:

1. **WelcomeStep**:
   - Shows a welcome message in the Project tool window
   - Introduces the zhaba character with a looking-around animation
   - Ensures the Project tool window is visible

2. **TaskDescriptionStep**:
   - Highlights the task description panel
   - Explains how to read task descriptions
   - Ensures the task description panel is visible

3. **CodeEditorStep**:
   - Focuses on the code editor
   - Explains how to work with code
   - May highlight specific editor features

4. **CheckSolutionStep**:
   - Highlights the check solution button
   - Explains how to verify solutions
   - May demonstrate the feedback process

5. **CourseViewStep**:
   - Focuses on the course structure view
   - Explains how to navigate between tasks and lessons
   - May highlight specific navigation features

## Animation System

The animation system has several key behaviors:

1. **Zhaba Character**:
   - The zhaba (frog) character guides the user through the interface
   - It has different animations for different states (looking, jumping, happy, sad)
   - It is positioned relative to the UI element being explained

2. **Transitions**:
   - Transitions are determined by the source and destination steps
   - Predefined transitions include:
     - `JumpRight`: For moving right (e.g., welcome to task description)
     - `JumpLeft`: For moving left (e.g., check solution to course view)
     - `JumpDown`: For moving down (e.g., code editor to check solution)
     - `HappyJumpDown`: For the final animation when completing the onboarding
     - `SadJumpDown`: For the final animation when skipping the onboarding

3. **Tooltips**:
   - Tooltips are positioned relative to the UI element being explained
   - They can be positioned above, below, or centered on the element
   - They include text, buttons, and optional step numbers

## Usage Statistics

The module collects anonymous usage statistics:

1. **Events Tracked**:
   - `uiOnboardingStarted`: When the user starts the onboarding
   - `uiOnboardingFinished`: When the user completes the onboarding
   - `uiOnboardingSkipped`: When the user skips the onboarding
   - `uiOnboardingRelaunched`: When the user restarts the onboarding

2. **Data Collected**:
   - Step index and ID when skipping
   - Relaunch location when restarting

## Error Handling

The module includes error handling for various scenarios:

1. **Step Availability**:
   - If a step is not available, it is skipped
   - The onboarding continues with the next available step

2. **UI Element Visibility**:
   - If a required UI element is not visible, the step is skipped
   - This ensures the onboarding doesn't get stuck

3. **Disposal**:
   - All components are properly disposed when the onboarding is finished
   - This prevents memory leaks and ensures clean termination