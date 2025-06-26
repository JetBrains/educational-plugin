# IDE Onboarding Feature - Active Context

## Current Implementation State

### Core Components
1. EduUiOnboardingService
   - Project-level service managing onboarding state
   - Handles onboarding flow execution
   - Maintains tour progress state
   - Coordinates step execution

2. Onboarding Steps
   Current implementation includes five sequential steps:
   1. Welcome Step - Initial introduction
   2. Task Description Step - Explaining task details
   3. Code Editor Step - Code editing interface guide
   4. Check Solution Step - Solution verification process
   5. Course View Step - Course navigation overview

### Implementation Details
- Coroutine-based asynchronous execution
- EDT (Event Dispatch Thread) handling for UI updates
- Atomic state management for tour progress
- Disposable service lifecycle management
- Step availability verification before execution

### Current Features
1. Tour Progress Management
   - Atomic boolean state tracking
   - Prevention of concurrent tours
   - Proper cleanup on completion

2. Step Execution
   - Sequential step ordering
   - Dynamic step availability checking
   - Flexible step configuration

3. Animation Support
   - Animation data loading
   - Executor-based animation control
   - Project-scoped animation context

## Active Development Areas
1. Step Implementation
   - Individual step behavior definition
   - Step-specific UI interactions
   - Progress tracking per step

2. User Interaction
   - Tour flow control
   - Progress indication
   - Step transitions

3. State Management
   - Tour state persistence
   - Progress tracking
   - Error handling