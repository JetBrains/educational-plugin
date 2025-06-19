# IDE Onboarding Module Overview

## Purpose

The IDE Onboarding module provides an interactive, guided tour of the Educational Plugin's features for new users. 
It helps users understand how to navigate the IDE, read task descriptions, work with the code editor, check solutions, and use the course view.

## Key Features

1. **Interactive Tour**: Provides a step-by-step guided tour with interactive elements
2. **Animated Guide Character**: Uses an animated "zhaba" (frog) character to guide users through the interface
3. **Contextual Help**: Shows tooltips and explanations in the context of the actual IDE interface
4. **Customizable Steps**: Supports a configurable sequence of onboarding steps
5. **Transition Animations**: Provides smooth transitions between different steps of the tour

## User Experience

The onboarding experience is designed to be:
- **Engaging**: Uses animations and a character guide to make learning fun
- **Informative**: Provides clear explanations of key features
- **Progressive**: Introduces features in a logical sequence
- **Non-intrusive**: Can be skipped or restarted at any time

## Integration Points

The IDE Onboarding module integrates with:
- **Project Tool Window**: For introducing the course structure
- **Editor**: For demonstrating code editing features
- **Task Description Panel**: For explaining how to read task descriptions
- **Check Solution Button**: For showing how to verify solutions

## Usage Statistics

The module collects anonymous usage statistics to help improve the onboarding experience:
- When users start the onboarding
- When users complete the onboarding
- When users skip the onboarding
- When users restart the onboarding

## Extension Points

The module provides an extension point for adding custom onboarding steps:
- `com.intellij.ide.eduUiOnboarding.step`: For registering custom onboarding steps